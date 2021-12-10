import os
import glob
import sys


def instanceStatistics(biasFile):
    targetFile = biasFile.replace(".bias", ".target")

    bias = readBias(biasFile)
    target = readTarget(targetFile)

    instance = os.path.basename(biasFile.replace(".bias", ""))
    numTasks = bias["nbVars"]//2
    numRelations = len(bias["gamma"])
    numClauses = len(target["clauseSize"])
    avgClauseSize = (sum(target["clauseSize"])/numClauses)
    numPwr = sum((c == numRelations for c in target["clauseSize"]))
    pairsWithoutRelation = (100*sum((c == numRelations for c in target["clauseSize"]))/numClauses)
    density = avgClauseSize/13
    print(f"{instance};{numTasks};{numRelations};{numClauses};{avgClauseSize:.1f};{pairsWithoutRelation:.2f};{density:.2f};{numPwr}")


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


def readTarget(targetFile):
    target = {"clauseSize": []}
    with open(targetFile, "r") as f:
        for r in f:
            parts = r.strip().split()
            var1 = parts[-2]
            var2 = parts[-1]
            relations = parts[:-2]
            target["clauseSize"].append(len(relations))

    return target



if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Provide a directory with instances as a parameter")
        sys.exit(1)

    dir = sys.argv[1]

    print("instance;numTasks;gammaSize;numClauses;avgClauseSize;percPairsWithoutRestriction;density;pairsWithoutRestriction")
    for f in sorted(glob.glob(f"{dir}/tasks*.bias")):
        instanceStatistics(f)
