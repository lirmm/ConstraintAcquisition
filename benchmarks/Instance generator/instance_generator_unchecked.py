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


def generateInstance(numTasks, maxRelsToSet, fullEdges, target_dir):
    nodes = [[str(i), ["ProperInterval"]] for i in range(numTasks)]
    edges = {}

    all_relations = set(RELMAP.values())
    # all_relations = set(["B", "M", "BI"])  # "O", "E", "D", "S", "F"

    for i in range(numTasks):
        for j in range(i+1, numTasks):
            relsToSet = random.randint(1, maxRelsToSet)
            edges[(i, j)] = random.sample(all_relations, k=relsToSet)

    if fullEdges == 1.0:
        for i in range(numTasks):
            for j in range(i+1, numTasks):
                edges[(i, j)] = all_relations
    else:
        for _ in range(int(fullEdges*numTasks*(numTasks-1)/2)):
            i, j = sorted(random.sample(range(numTasks), 2))
            edges[(i, j)] = all_relations

    inst = {
        "name": "generate",
        "algebra": "Linear_Interval_Algebra",
        "nodes": nodes,
        "edges": [[str(k[0]), str(k[1]), "|".join(v)] for k, v in edges.items()]
    }

    # print(inst)
    net = qr.Network(algebra_path=alg_path, network_dict=inst)
    # is_ok = net.propagate()

    num_relations = 0
    num_clauses = 0
    num_unrestricted = 0

    for i in range(numTasks):
        for j in range(i+1, numTasks):
            num_clauses += 1
            tail, head, _ = net.get_edge(str(i), str(j), return_names=False)
            # print(net.edges[head, tail]['constraint'])
            num_relations += len(str(net.edges[head, tail]['constraint']).split("|"))
            num_unrestricted += len(str(net.edges[head, tail]['constraint']).split("|")) == len(RELMAP)

    num_unrestricted += (numTasks*(numTasks-1)/2) - num_clauses

    density = num_relations/num_clauses/len(RELMAP.values())
    print(density, num_unrestricted)

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
        for i in range(numTasks):
            for j in range(i+1, numTasks):
                tail, head, _ = net.get_edge(str(i), str(j), return_names=False)
                rels = " ".join((RELMAP_INV[k] for k in str(net.edges[head, tail]['constraint']).split("|")))
                f.write(f"{rels} {head.name} {tail.name}\n")


if __name__ == "__main__":
    target_dir = "rcpsp/"
    if len(sys.argv) < 4:
        print("Provide parameters")
        sys.exit(1)

    num_tasks = int(sys.argv[1])
    maxRelsToSet = int(sys.argv[2])
    fullEdges = float(sys.argv[3])
    generateInstance(num_tasks, maxRelsToSet, fullEdges, target_dir)
