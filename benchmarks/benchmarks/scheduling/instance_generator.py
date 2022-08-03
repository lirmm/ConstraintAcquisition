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


def generateInstance(numTasks, minDensity, maxDensity, num_unrestricted, target_dir):
    nodes = [[str(i), ["ProperInterval"]] for i in range(numTasks)]
    edges = {}

    all_relations = set(RELMAP.values())
    # all_relations = set(["B", "M", "BI"])  # "O", "E", "D", "S", "F"
    maxRelsToSet = 1  #len(all_relations)-1 #12

    fixed = set()

    for i in range(10):
        t1, t2 = sorted(random.sample(range(numTasks), 2))  
        edges[(t1, t2)] = set(all_relations)
        fixed.add((t1, t2))

    is_ok = False
    loop_counter = 0
    while not is_ok:
        loop_counter += 1
        t1, t2 = sorted(random.sample(range(numTasks), 2))

        if (t1, t2) in fixed:
            # do nothing
            pass
        elif (t1, t2) in edges:
            relsToSet = random.randint(1, min(maxRelsToSet, len(edges[(t1, t2)])))
            edges[(t1, t2)] = random.sample(edges[(t1, t2)], relsToSet)
        else:
            relsToSet = random.randint(1, maxRelsToSet)        
            edges[(t1, t2)] = random.sample(all_relations, relsToSet)

        inst = {
            "name": "generate",
            "algebra": "Linear_Interval_Algebra",
            "nodes": nodes,
            "edges": [[str(k[0]), str(k[1]), "|".join(v)] for k, v in edges.items()]
        }

        if len(edges) < 0.1*numTasks*(numTasks-1)/2:
            continue

        # print(inst)
        net = qr.Network(algebra_path=alg_path, network_dict=inst)
        is_ok = net.propagate()

        if not is_ok:
            edges = {}
            #maxRelsToSet = random.randint(2, len(all_relations)-1)
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
        is_ok = (actual_unrestricted == num_unrestricted) and (minDensity <= density <= maxDensity)

        if density < minDensity:
            edges = {}
            #maxRelsToSet = random.randint(2, len(all_relations)-1)
            continue

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
        for head in net.nodes:
            for tail in net.neighbors(head):
                if int(head.name) >= int(tail.name):
                    continue
                rels = " ".join((RELMAP_INV[k] for k in str(net.edges[head, tail]['constraint']).split("|")))
                f.write(f"{rels} {head.name} {tail.name}\n")


if __name__ == "__main__":
    target_dir = "rcpsp/"
    if len(sys.argv) < 5:
        print("Provide parameters")
        sys.exit(1)

    num_tasks = int(sys.argv[1])
    minDensity = float(sys.argv[2])
    maxDensity = float(sys.argv[3])
    num_unrestricted = int(sys.argv[4])
    generateInstance(num_tasks, minDensity, maxDensity, num_unrestricted, target_dir)
