import random
import os
import glob
os.environ["PYPROJ"] = os.getcwd()
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


def generateInstance(numTasks, percEdgesWithRelation, maxRelsToSet, num_unrestricted, target_dir):
    nodes = [[str(i), ["ProperInterval"]] for i in range(numTasks)]

    all_relations = set(RELMAP.values())

    is_ok = False
    loop_counter = 0
    while not is_ok:
        if maxRelsToSet == 1:
            all_relations = set(["B", "BI"]) # , , "M" "BI", "MI", "O", "D", "DI", "S", "F", 
            t1 = -1
            t2 = 0
        elif maxRelsToSet == 2:
            # all_relations = set(["B", "M", "O", "E", "D", "S", "F"]) # , , "M" "BI", "MI", "O", "D", "DI", "S", "F",
            all_relations = set(["B", "M", "O", "E", "D", "S", "F"])
            t1 = -1
            t2 = 0

        edges = []
        rels = random.choices(all_relations, k=numTasks*(numTasks-1)//2)
        loop_counter += 1
        for _ in range(round(percEdgesWithRelation*numTasks*(numTasks-1)/2)):
            if maxRelsToSet <= 2:
                t1 += 1
                t2 += 1
                if t2 == numTasks:
                    break
            else:
                t1, t2 = sorted(random.sample(range(numTasks), 2))
            relsToSet = random.randint(1, maxRelsToSet)
            relations = random.sample(all_relations, relsToSet)
            edges.append([
                str(t1),
                str(t2),
                "|".join(relations)
            ])

        inst = {
            "name": "generate",
            "algebra": "Linear_Interval_Algebra",
            "nodes": nodes,
            "edges": edges
        }
        # print(inst)
        net = qr.Network(algebra_path=alg_path, network_dict=inst)
        is_ok = net.propagate()
    
        if not is_ok:
            edges = []
            continue

        num_relations = 0
        num_clauses = 0
        actual_unrestricted = 0

        for head in net.nodes:
            for tail in net.neighbors(head):
                if int(head.name) >= int(tail.name):
                    continue
                num_clauses += 1
                num_relations += len(str(net.edges[head, tail]['constraint']).split("|"))
                actual_unrestricted += len(str(net.edges[head, tail]['constraint']).split("|")) == len(RELMAP)

        actual_unrestricted += (numTasks*(numTasks-1)/2) - num_clauses

        density = num_relations/num_clauses/len(RELMAP.values())
        print(density, actual_unrestricted)
        is_ok = (actual_unrestricted == num_unrestricted)

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
    if len(sys.argv) < 5:
        print("Provide parameters")
        sys.exit(1)

    num_tasks = int(sys.argv[1])
    ratio = float(sys.argv[2])
    relstoset = int(sys.argv[3])
    num_unrestricted = int(sys.argv[4])
    generateInstance(num_tasks, ratio, relstoset, num_unrestricted, target_dir)
