import random
import os
import glob
os.environ["PYPROJ"] = os.environ["PYPROJ"] = os.getcwd()
path = os.path.join(os.getenv('PYPROJ'), 'qualreas')

import sys
sys.path.insert(0, os.path.join(os.getenv('PYPROJ'), 'qualreas', 'Source'))
import qualreas as qr


# Allen Interval
# alg = qr.Algebra(os.path.join(path, "Algebras/Linear_Interval_Algebra.json"))
alg_path = os.path.join(path, "Algebras")

RELMAP = {
    "PrecedesXY": "B",
    "IsPrecededXY": "BI",
    "MeetsXY": "M",
    "IsMetXY": "MI",
    "OverlapsXY": "O",
    "IsOverlappedXY": "OI",
    "FinishXY": "F",
    "IsFinishedXY": "FI",
    "DuringXY": "D",
    "ContainsXY": "DI",
    "StartsXY": "S",
    "IsStartedXY": "SI",
    "ExactXY": "E",
}
RELMAP_INV = {v: k for k, v in RELMAP.items()}

def readBias(biasFile):
    bias = {}
    with open(biasFile, "r") as f:
        readGamma = False

        for r in f:
            if r.startswith("nbVars"):
                bias["nbVars"] = int(r.split(" ")[1])

            elif r.startswith("domainSize"):
                minDom, maxDom = r.split(" ")[1:]
                bias["minDom"] = int(minDom)
                bias["maxDom"] = int(maxDom)

            elif r.startswith("Gamma"):
                readGamma = True
                bias["gamma"] = []

            elif readGamma:
                bias["gamma"].append(r.strip())

    return bias


def generateInstance(numTasks, percEdgesWithRelation, maxRelsToSet, target_dir):
    nodes = [[str(i), ["ProperInterval"]] for i in range(numTasks)]

    all_relations = set(RELMAP.values())

    is_ok = False
    loop_counter = 0
    while not is_ok:
        if maxRelsToSet == 1:
            all_relations = ["B", "M", "E"] # , , "M" "BI", "MI", "O", "D", "DI", "S", "F", 
            t1 = -1
            t2 = 0
        elif maxRelsToSet == 2:
            # all_relations = set(["B", "M", "O", "E", "D", "S", "F"]) # , , "M" "BI", "MI", "O", "D", "DI", "S", "F",
            all_relations = set(["B", "M", "O", "E", "D", "S", "F"])
            t1 = -1
            t2 = 0

        edges = []
        is_init_ok = False

        while not is_init_ok:
            while t2 < numTasks-1:
                t1 += 1
                t2 += 1

                edges.append([
                    str(t1),
                    str(t2),
                    "|".join((random.choice(all_relations),))
                ])

            inst = {
                "name": "generate",
                "algebra": "Linear_Interval_Algebra",
                "description": "Description",
                "nodes": nodes,
                "edges": edges
            }
            # print(inst)
            net = qr.Network(algebra_path=alg_path, network_dict=inst)
            is_init_ok = net.propagate()

        num_relations = 0
        num_clauses = 0
        num_unrestricted = 0

        for head in net.nodes:
            for tail in net.neighbors(head):
                if int(head.name) >= int(tail.name):
                    continue
                num_clauses += 1
                num_relations += len(str(net.edges[head, tail]['constraint']).split("|"))
                num_unrestricted += len(str(net.edges[head, tail]['constraint']).split("|")) == len(RELMAP)

        num_unrestricted += (numTasks*(numTasks-1)/2) - num_clauses
        density = num_relations/num_clauses/len(RELMAP.values())
        print(density, num_unrestricted)
        
        net_copy = net.mostly_copy()
        is_ok = False

        while not is_ok:
            t1, t2 = sorted(random.sample(range(numTasks), 2))

            tail_node, head_node, _ = net_copy.get_edge(str(t1), str(t2), return_names=False)
            net_copy.set_constraint(tail_node, head_node, net_copy.algebra.relset(list(RELMAP.values())))  #net_copy.algebra.relset((r,))

            net_copy2 = net_copy.mostly_copy()

            is_ok = net_copy2.propagate()

            if not is_ok:
                net_copy = net.mostly_copy()
                continue

            num_relations = 0
            num_clauses = 0
            num_unrestricted = 0

            for head in net_copy2.nodes:
                for tail in net_copy2.neighbors(head):
                    if int(head.name) >= int(tail.name):
                        continue
                    num_clauses += 1
                    num_relations += len(str(net_copy2.edges[head, tail]['constraint']).split("|"))
                    num_unrestricted += len(str(net_copy2.edges[head, tail]['constraint']).split("|")) == len(RELMAP)

            num_unrestricted += (numTasks*(numTasks-1)/2) - num_clauses
            density = num_relations/num_clauses/len(RELMAP.values())
            print(density, num_unrestricted, num_relations)
            is_ok = num_unrestricted == 10 and num_relations == 290

            if num_unrestricted > 10:
                net_copy = net.mostly_copy()

    net = net_copy2  # keep working network
    # print(net.summary())

    num_relations = 0
    num_clauses = 0

    for head in net.nodes:
        for tail in net.neighbors(head):
            if head == tail:
                continue
            num_clauses += 1
            num_relations += len(str(net.edges[head, tail]['constraint']).split("|"))

    density = num_relations/num_clauses/len(RELMAP.values())
    instance_name = f"tasks{numTasks}_{density:.2f}"
    print(instance_name)

    file_exists = True
    iter = 0
    while file_exists:
        iter += 1
        bias_file = os.path.join(target_dir, f"{instance_name}_{iter}.bias")
        file_exists = os.path.exists(bias_file)

    target_file = os.path.join(target_dir, f"{instance_name}_{iter}.target")

    with open(bias_file, "w") as f:
        f.write(f"nbVars {numTasks*2}\n")
        f.write(f"domainSize 1 {20*numTasks+numTasks}\n")
        f.write("\n")
        f.write("Gamma\n")
        for v in RELMAP_INV.values():
            f.write(v + "\n")

    with open(target_file, "w") as f:
        done = set()
        for head in net.nodes:
            for tail in net.neighbors(head):
                if int(head.name) >= int(tail.name):
                    continue
                rels = " ".join((RELMAP_INV[k] for k in str(net.edges[head, tail]['constraint']).split("|")))
                f.write(f"{rels} {head.name} {tail.name}\n")
                done.add(head)


if __name__ == "__main__":
    target_dir = "rcpsp/"
    if len(sys.argv) < 4:
        print("Provide parameters")
        sys.exit(1)

    num_tasks = int(sys.argv[1])
    ratio = float(sys.argv[2])
    relstoset = int(sys.argv[3])
    generateInstance(num_tasks, ratio, relstoset, target_dir)
    # generateInstance(10, 0.5, 1, target_dir)