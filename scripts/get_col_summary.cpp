// A C++ program to summarize data for a specific column from multiple
// independent simulation runs.  The aggregate data file to be
// processed and the column whose statistics is to be reported must be
// specified as command-line arguments.  This program requires g++
// 4.9.2 or later.  It is compiled as:
//
// $ g++ -O3 -std=c++11 -g -Wall get_col_summary.cpp -o get_col_summary

// The data used by this script is obtained by concatenating outputs
// from multiple runs together as suggested below:
// $ find . -name "java_output_?.txt" -exec grep "^[0-9]" {} \; > aggregate_java_output.txt

// The aggregate output data is assumed to be a tab-separated file in
// the form:
//
// day     diversity       tmrca   netau   serialInterval  antigenicDiversity      N       S       I       R       cases
// 0       0.000   0.000   0.274   0.000   0.000   10000   9900    100     0       0
// 5       0.017   0.014   0.274   0.002   0.024   9955    6442    3513    0       5098
// 10      0.041   0.027   0.274   0.003   0.036   9899    7272    2627    0       2203
// 15      0.063   0.041   0.274   0.003   0.042   9843    8071    1772    0       1432
//

#include <iostream>
#include <string>
#include <fstream>
#include <sstream>
#include <cmath>
#include <set>

// The Z-value from student t-distribution 2-sided for the given
// number of entries from
// https://en.wikipedia.org/wiki/Student%27s_t-distribution
const double z = 2.403;   // 50 degrees for 51 inputs
// const double z = 1.96;   // 249 degrees for 250 inputs

// A pair to hold the sum (double), variance, count (int) of a given
// column for each day.
class DayStats {
public:
    int day;
    double sum;
    double mean;
    double var;   // this is variance * count
    int count;
    double min;
    double max;
    
    // Simple convenience constructor
    DayStats(int day, double value = 0) :
        day(day), sum(value), mean(value), var(0), count(1), min(value),
        max(value) {}
    
    // Operator += to add a value to this stats object:
    DayStats operator+(double value) const {
        const double delta = value - mean;
        DayStats ret(day);
        ret.count = count + 1;
        ret.sum   = sum + value;
        ret.mean  = mean + (delta / ret.count);
        ret.var   = var  + (delta * (value - ret.mean));
        ret.min   = std::min(min, value);
        ret.max   = std::max(max, value);
        return ret;
    }
    
    // Comparator used by set to order entries based on day
    bool operator<(const DayStats& other) const {
        return (day < other.day);
    }

    double sd() const {
        return std::sqrt(var / count);
    }

    double ci() const {
        return (z * sd()) / sqrt(count);
    }
};

   
// Stream insertion operator to print values in this entry
std::ostream& operator<<(std::ostream& os, const DayStats& ds) {
    const std::string sep = ", ";
    os << ds.day   << sep << ds.count << sep << ds.mean << sep
       << ds.sd() << sep << ds.min   << sep << ds.max  << sep
       << ds.sum   << sep << ds.ci();
    return os;
}

// A set to hold the stats for a given day. The key into the hash
// map is the day number (0, 5, 10, etc.)
using StatSet = std::set<DayStats>;

// Method to process a line and add entries into the stat map.
void aggregate(StatSet& stats, const std::string& line, const int column) {
    std::istringstream is(line);    
    int day;
    double value;
    // Read day from line.
    is >> day;
    // Skip over values for columns until we get the one we want.
    for (int col = 0; (col < column); col++) {
        is >> value;
    }
    // Update stats for the given day with the value.
    StatSet::iterator it = stats.find(DayStats(day));
    if (it == stats.end()) {
        // New entry for this day
        stats.insert(DayStats(day, value));
    } else {
        // Accumulate stats for this day into existing entry.
        const DayStats stat = *it + value;
        stats.erase(it);
        stats.insert(stat);
    }
}

void print(const StatSet& stats, const int startDay = 0,
           std::ostream& os = std::cout) {
    os << "#day, count, mean, SD, main, max, sum, 95%ci\n";
    for (auto entry : stats) {
        if (entry.day >= startDay) {
            os << entry << std::endl;
        }
    }
}

void processFile(const std::string& filePath, const int column,
                 const int startDay = 0) {
    std::ifstream dataFile(filePath);
    if (!dataFile.good()) {
        std::cerr << "Error loading data from " << dataFile << std::endl;
        return;
    }
    // Process line-by-line and aggregate information
    StatSet stats;
    std::string line;
    while (std::getline(dataFile, line)) {
        if (line.size() > 10) {
            aggregate(stats, line, column);
        }
    }
    // Print the statistics.
    print(stats, startDay);
}

int main(int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Specify <DataFile> <ColNum> [<OptionalStartDay>]\n";
        return 1;
    }
    const int startDay = (argc > 3) ? std::stoi(argv[3]) : 0;
    processFile(argv[1], std::stoi(argv[2]), startDay);
    return 0;
}
