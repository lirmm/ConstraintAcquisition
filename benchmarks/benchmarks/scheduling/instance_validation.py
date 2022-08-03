import glob
import math
import os
from decimal import Decimal
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


def processInstance(biasFile):
    targetFile = biasFile.replace(".bias", ".target")
    instance_name = os.path.basename(biasFile.replace(".bias", ""))
    print(f"{instance_name}", end="", flush=True)

    bias = readBias(biasFile)

    nodes = []
    numTasks = bias["nbVars"]//2

    for i in range(numTasks):
        nodes.append([str(i), ["ProperInterval"]])

    edges = []

    with open(targetFile, "r") as f:
        for r in f:
            parts = r.strip().split()
            t1 = parts[-2]
            t2 = parts[-1]
            relations = parts[:-2]
            edges.append([
                str(t1),
                str(t2),
                "|".join((RELMAP[s] for s in relations))
            ])

    inst = {
        "name": instance_name,
        "description": "desc",
        "algebra": "Linear_Interval_Algebra",
        "nodes": nodes,
        "edges": edges
    }
    # print(inst)
    net = qr.Network(algebra_path=alg_path, network_dict=inst)
    ok = net.propagate()
    print(f"", end="", flush=True)

    if ok:
        rel_lens = []

        for i in range(numTasks):
            for j in range(i+1, numTasks):
                tail, head, _ = net.get_edge(str(i), str(j), return_names=False)
                rel_lens.append(len(str(net.edges[head, tail]['constraint']).split("|")))

        candidates = Decimal(math.prod(rel_lens))        
    else:
        candidates = 0

    print(f";{ok};{candidates:E}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Provide a directory with instances as a parameter")
        sys.exit(1)

    dir = sys.argv[1]

    print("instance;feasible;solutionCandidates")
    for f in sorted(glob.glob(f"{dir}/tasks*.bias")):
        processInstance(f)
        
