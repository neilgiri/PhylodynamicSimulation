#!/usr/bin/python

# This script uses a given phylogenetic tree and creates clusters from
# it based on distances between the leaf nodes in the cladogram.  The
# distance at which clusters must be cut is passed in as parameter.
#

from ete2 import Tree
import sys

# Top-level method to initiate clustering. The parameters are set to
# default for cutoff value (1.5%) and tree distance (1600 nt for 90%
# H5 full frame)
def cluster(root, treeDist = 1, cutoff = 27):
    if len(root.get_children()) == 0:
        # Empty tree!
        return ([], [], [])
    # The tree has at least one node. Safe to process.
    clusterList    = [[]]
    interClustDist = []
    intraClustDist = [0]
    # Recursively process all the child nodes in the tree
    createClusters(root, cutoff, treeDist, 0, [0], clusterList,
                   interClustDist, intraClustDist)
    # Need to update average intra-cluster size for last cluster
    intraClustDist[-1] /= len(clusterList[-1])
    # Return results back to the caller
    return (clusterList, interClustDist, intraClustDist)


def createClusters(node, cutoff, treeDist, currDist, distToPrevNode,
                   clusterList, interClustDist, intraClustDist):
    nodeDist = node.dist
    distToPrevNode[0] += nodeDist # same for leaf and non-leaf cases
    if len(node.get_children()) > 0:
        # non-leaf node. process all child nodes.
        for childNode in node.get_children():
            createClusters(childNode, cutoff, treeDist, currDist + nodeDist,
                           distToPrevNode, clusterList, interClustDist,
                           intraClustDist)
        # Update distances to next node when recursion unwinds (as it
        # is reset in the else part below).
        distToPrevNode[0] += nodeDist
    else:
        # leaf node
        if ((distToPrevNode[0] / treeDist) > cutoff) and \
        (len(clusterList[-1]) > 0):
            # This node is too far from adjaent node.  First, track
            # inter and intra cluster distances
            interClustDist.append(distToPrevNode[0] / treeDist)
            intraClustDist[-1] /= len(clusterList[-1])
            intraClustDist.append(0);
            #Create new cluster
	    clusterList.append([]);
        # Add leaf node to the current cluster.
        clusterList[-1].append(node)
        node.clusterID = len(clusterList)
        distToPrevNode[0]   = nodeDist # reset distance to adjacent node
        intraClustDist[-1] += nodeDist / treeDist


# Convenience method to process command-line argument value
def getParam(args, index, defVal):
    retVal = defVal
    if (len(args) > index):
        retVal = float(args[index])
    return retVal


def getAvg(list):
    avg = 0
    if (len(list) > 0):
        avg = sum(list) / len(list)
    return avg


def main():
    # Main part of the code
    if (len(sys.argv) < 2):
        print("Specify valid input phylip files to process.")
        print("The reference phylogenetic tree must be the first command-line")
        print("argument followed by an optional distance scale and cutoff")
        print("Example: " + sys.argv[0] + " egypt.ph 1841 0.015")
    else:
        # Load the phylogenetic tree from the specified file
        treeFile = open(sys.argv[1], 'r')
        tree     = Tree(treeFile.readline())
        treeFile.close()
        # Have the main clustering method do rest of the tasks
        (clusters, interClustDists, intraClustDists) = \
        cluster(tree, getParam(sys.argv, 2, 1),
                getParam(sys.argv, 3, 27))
        print "Cluster count      : ", len(clusters)
        print "Inter-Cluster dists: ", interClustDists
        print "Avg Inter-cluster  : ", getAvg(interClustDists)
        print "Intra-cluster dists: ", intraClustDists
        print "Avg Intra-cluster  : ", getAvg(intraClustDists)


# Call the main method to perform the necessary checks.
if __name__ == '__main__':
    main()

# End of script
