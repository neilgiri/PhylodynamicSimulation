#!/usr/bin/python

# This script compares the following characteristics of two
# phylogenetic trees and generates error metrics:
#

from ete2 import Tree
from collections import Counter
from collections import OrderedDict
from itertools import izip_longest

import sys
import csv
import os

from Cluster import cluster, getAvg

def error(theor, exp):
    """A simple convenience method to generate absolute percentage error
    between theoretical value and an exptected value.
    """
    if (theor == 0) and (exp == 0):
        return 0.0
    return (abs(theor - float(exp)) / max(theor, exp)) * 100.0

#---------[   methods for comparing number of childern / node   ]----------

def calc_node_size_percent(tree, MaxChildren = 6):
    """Calculates the percentage of nodes in the tree with a given number
       of children. The number of children to track is indicated by
       MaxChildren parameter.
    """
    freqs = Counter()
    # Track number of nodes with a given number of child nodes.  That
    # is track how many nodes have just 0 child nodes (leaf nodes),
    # how many nodes have 1 child node, etc.
    for node in tree.get_descendants():
        num_child = node.get_children()
        freqs[len(num_child)] += 1
    # Now convert the totals into percentages so that we can compare
    # two trees.
    total = sum(freqs.itervalues())
    if total <= 0:
        total = 1.0
    for i in range(0, MaxChildren):
        percent = float(freqs[i]) / float(total)
        freqs[i] = percent * 100.0
    # Return the percentage of nodes with different number of children
    # back to the caller.
    return freqs


def output_children(country, refTree, simTree, outputDir, MaxChildren = 6):
    # If simulated tree is empty (which can happen at poor settings)
    # just report 100% error and get out.
    if (len(simTree.get_children()) == 0) or (len(refTree.get_children()) == 0):
        print "node_size", 100,
        return
    # Calculate node size frequency for the 2 trees
    simChildFreq = calc_node_size_percent(simTree)
    refChildFreq = calc_node_size_percent(refTree)
    # Compute the difference in percentage-sizes of the nodes in the
    # two trees.  Compute total errors only if percentages are over 1%
    # in both (or both are zero)
    error_list = []
    for k in range(0, MaxChildren):
        if ((refChildFreq[k] == simChildFreq[k]) or
            (refChildFreq[k] > 1) or (simChildFreq[k] > 1)):
            error_list.append(error(refChildFreq[k], simChildFreq[k]))
    totalError = sum(error_list) / len(error_list)
    print "node_size", totalError,
    # Write the frequency information to data file for plotting.
    if (len(outputDir) > 0):
        file = open(outputDir + "/" + country + "_children.dat", 'w')
        file.write('Children Simulation EpiFlu\n')
        for i in range(0, MaxChildren):
            file.write(str(i) + ' ' + str(simChildFreq[i]) + ' ' +
                       str(refChildFreq[i]) + '\n')
        file.close()

#----[   methods for comparing inter- and intra-cluster  distances   ]----

def output_clust_dists(country, refTree, simTree, outputDir):
    # Compute cluster information for each tree
    (refClusts, refInterClustDists, refIntraClustDists) = cluster(refTree)
    (simClusts, simInterClustDists, simIntraClustDists) = cluster(simTree)
    # Print difference in number of clusters
    print "ref-clusters", len(refClusts), "sim-clusters", len(simClusts),
    # Get average Inter-Cluster Distances (ICD)
    avgRefICD = getAvg(refInterClustDists)
    avgSimICD = getAvg(simInterClustDists)
    print "ref-ICD", avgRefICD, "sim-ICD", avgSimICD,
    # Print percentage difference in intra-cluster distances
    print "inter-clust-dists", error(avgRefICD, avgSimICD),
    print "intra-clust-dists", error(getAvg(refIntraClustDists),\
                                     getAvg(simIntraClustDists)),

#---------[   methods for comparing average leaf distances   ]----------

def fill_distances(dict, tree, key):
    """Helper method to determine the distances (that is number of
    nucleotide differences) of each leaf node in the tree to the root
    of the tree.  The list is added to a given dictionary using a
    specified key.
    """
    for node in tree.get_leaves():
        dist = node.dist
        dict.setdefault(key, []).append(dist)

def output_distances(country, refTree, simTree, outputDir):
    # If simulated tree is empty (which can happen at poor settings)
    # just report 100% error and get out.
    if (len(simTree.get_children()) == 0) or (len(refTree.get_children()) == 0):
        print "distances", 100,
        return    
    # Compute distance of each leaf node to the root of the tree
    dict = OrderedDict()
    fill_distances(dict, simTree, 'Simulation')
    fill_distances(dict, refTree, 'EpiFlu')    
    # Compute the average depth/distance of leaf nodes from root
    simAvgLeafDepth = float(sum(dict['Simulation'])) / len(dict['Simulation'])
    refAvgLeafDepth = float(sum(dict['EpiFlu']))     / len(dict['EpiFlu'])
    diffInDepth     = error(refAvgLeafDepth, simAvgLeafDepth)
    # Output the difference in average leaf depth
    print "distances", diffInDepth,
    # Write the frequency information to data file for plotting.
    if (len(outputDir) > 0):
        outFilePath = outputDir + "/" + country + "_distances.dat"
        with open(outFilePath, 'wb') as file:
            writer = csv.writer(file, delimiter=' ')
            writer.writerow(dict.keys())
            # Write each row to the output file.
            for row in izip_longest(*dict.values(), fillvalue=''):
                writer.writerow(list(row))
        file.close()

#---------[   methods for comparing average node depths   ]----------

def node_depth(tree, node):
    """Convenience recursive method to determine depth of a given node in
    a given tree.
    """
    if (tree.get_tree_root() == node):
        return 0
    # Determine the deepest child node
    d = 0
    for n in node.get_children():
        d = max(d, node_depth(tree, n))
    # Returns depth based on deepest child node of this node.
    return d + 1

def calc_node_depth_percent(tree, MaxDepth = 11):
    """Calculates the percentage of nodes in the tree at different depths
    from the root."
    """
    # Compute frequency of nodes with a given depth value.
    freqs = Counter()
    for node in tree.get_descendants():
        depth = node_depth(tree, node)
        freqs[depth] += 1
    total = sum(freqs.itervalues())
    if total <= 0:
        total = 1.0
    # Convert raw counts to percentages to enable comparisons.
    for i in range(0, MaxDepth):
        percent = float(freqs[i]) / float(total)
        freqs[i] = percent * 100.0
    # Return the percentage of nodes at different depths
    return freqs


def output_node_depths(country, refTree, simTree, outputDir, MaxDepth = 11):
    # If simulated tree is empty (which can happen at poor settings)
    # just report 100% error and get out.
    if (len(simTree.get_children()) == 0) or (len(refTree.get_children()) == 0):
        print "distances", 100,
        return    
    # Compute percentage of nodes with depths 0 to MaxDepth
    refNodeDepths = calc_node_depth_percent(refTree, MaxDepth)
    simNodeDepths = calc_node_depth_percent(simTree, MaxDepth)
    # Compute the difference in percentage-sizes of the nodes in the
    # two trees.
    error_list = []
    for k in range(0, MaxDepth):
        error_list.append(error(refNodeDepths[k], simNodeDepths[k]))
    totalError = sum(error_list) / len(error_list)
    print "depth", totalError,
    # Write the frequency information to data file for plotting.
    if (len(outputDir) > 0):
        file = open(outputDir + "/" + country + "_depth.dat", 'w')
        file.write('Depth Simulation EpiFlu\n')
        for i in range(0, MaxDepth):
            file.write(str(i) + ' ' + str(simNodeDepths[i]) + ' ' +
                       str(refNodeDepths[i]) + '\n')
        file.close()


#---------[   methods for coordinating various operations   ]----------

def process_trees(country, refTreeFileName, simTreeFileName, outDir):
    """
    Top-level method to process two phylogenetic trees and display
    comparative statistics about them.
    Parameters:
      refTreeFileName: Path to file that contains the reference tree.
      simTreeFileName: Path to file that contains the simulated tree.
    """
    # Load the reference phylogenetic tree.
    refFile = open(refTreeFileName, 'r')
    refTree = Tree(refFile.readline())
    refFile.close()
    
    # Load the simulated phylogenetic tree.
    simFile = open(simTreeFileName, 'r')
    simTree = Tree(simFile.readline())
    simFile.close()

    # Print comparisons on number of child nodes in the trees
    print country,
    output_clust_dists(country, refTree, simTree, outDir)    
    output_children   (country, refTree, simTree, outDir)
    output_distances  (country, refTree, simTree, outDir)
    output_node_depths(country, refTree, simTree, outDir)
    print


def main():
    # Main part of the code
    if (len(sys.argv) < 4) or (not os.path.isfile(sys.argv[2])) or (not os.path.isfile(sys.argv[3])):
        print("Specify country & 2 valid input phylip files to process.")
        print("The reference phylogenetic tree must be the first command-line")
        print("argument followed by the simulated phyloenetic tree.")
        print("Example: " + sys.argv[0] + " egypt egypt.ph out.trees temp_dir")
        print("NOTE: Output files are generated with country name as prefix")
        print("      in the specified output directory (last argument). If the")
        print("      output directory is not specified then files are not")
        print("      created (but errors are output on console)")
    else:
        outputDir = ""
        if (len(sys.argv) > 4):
            outputDir = sys.argv[4]
        # Process the specified trees
        process_trees(sys.argv[1], sys.argv[2], sys.argv[3], outputDir)


# Call the main method to perform the necessary checks.
if __name__ == '__main__':
    main()

# End of script
