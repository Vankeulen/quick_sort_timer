import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Quick {
    /**
     * Interface to pick a pivot for quicksort
     */
    interface PivotPicker {
        int getPivot(int[] arr, int left, int right);
    }

    /**
     * Interface to function that partitions array so that every element between left and right
     * is moved so that:
     * if it is less than the pivot value (array[pivot]) is placed in a lower index
     * if it is greater than the pivot value (array[pivot]) it is placed in a higher index
     * and returns the new position of the pivot value after partitioning
     */
    interface Partitioner {
        int partition(int[] array, int left, int right, int pivot);
    }

    /**
     * Interface to hold another sorting function
     * to use on runs of the array under a certain size
     */
    interface Subsort {
        void sort(int[] arr, int left, int right);
    }

    /**
     * Interface for generating test data.
     * This way we can vary the test data per test
     */
    interface Generator {
        int[] generate(int size);
    }

    /**
     * Data class for holding test configuration
     */
    static class TestSetup {
        public String name;
        public PivotPicker pivp;
        public Partitioner part;
        public int sst;
        public Subsort ssort;
        public Generator gen;

        public TestSetup(String name, PivotPicker pp, Partitioner pt) {
            this(name, pp, pt, -1, null, null);
        }

        public TestSetup(String name, PivotPicker pp, Partitioner pt, int sst, Subsort ss) {
            this(name, pp, pt, sst, ss, null);
        }

        public TestSetup(String name, PivotPicker pp, Partitioner pt, int sst, Subsort ss, Generator g) {
            this.name = name;
            this.pivp = pp;
            this.part = pt;
            this.sst = sst;
            this.ssort = ss;
            this.gen = g;
        }

        public TestSetup(TestSetup original, String newName) {
            this(newName, original.pivp, original.part, original.sst, original.ssort, original.gen);
        }
    }

    /**
     * Data class for holding test results
     */
    static class TestResult {
        public TestSetup setup;
        public List<Integer> dataSize;
        public List<Long> averageDuration;
        public List<List<Long>> durations;

        public TestResult() {
            dataSize = new ArrayList<>();
            averageDuration = new ArrayList<>();
            durations = new ArrayList<>();
        }
    }

    public static void main(String[] args) {

        List<TestSetup> tests = getTests();
        /// Data array sizes to run
        int[] sizes = { //10000
                25, 100, 250, 500,
                1000, 10000,

                // these big ones choke out the really bad sorts (always pivot on left/right)
                //5000, 10000, 15000, 20000
        };
        List<TestResult> results = new ArrayList(tests.size());
        /// Number of reps for each test
        int numIterations = 100;

        /// Loop over each test setup
        for (TestSetup setup : tests) {
            TestResult result = new TestResult();
            result.setup = setup;

            /// Run each size
            for (int dataSize : sizes) {

                /// All durations for this size
                List<Long> durations = new ArrayList(numIterations);
                /// Sum of durations for average
                long totalDuration = 0;

                /// Run test iterations and average for stability
                for (int i = 0; i < numIterations; i++) {
                    /// Generate new test data
                    int[] testArray = setup.gen.generate(dataSize); // generateArray(100, 999, dataSize);
                    int[] orig = Arrays.copyOf(testArray, testArray.length);

                    //System.out.println("\n\n\nGenerated new array");
                    //printArray(testArray);


                    // Run sort and only time the sort
                    long start = System.nanoTime();
                    sort(testArray, setup.pivp, setup.part, setup.sst, setup.ssort);
                    long end = System.nanoTime();

                    long duration = end - start;
                    totalDuration += duration;
                    durations.add(duration);

                    // Check for errors and print out of order elements for testing
                    try {
                        for (int k = 1; k < testArray.length; k++) {
                            if (testArray[k] < testArray[k - 1]) {
                                throw new RuntimeException("During test " + setup.name +
                                        "\n\tElements "
                                        + " array[" + k + "] = " + testArray[k]
                                        + " array[" + (k - 1) + "] = " + testArray[k - 1]
                                        + " are out of order ");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                        System.out.println("Array was ");
                        printArray(orig);
                        System.out.println("Array is ");
                        printArray(testArray);
                    }

                }

                /// Record the result for the current size
                result.dataSize.add(dataSize);
                result.durations.add(durations);
                result.averageDuration.add(totalDuration / numIterations);
            }

            System.out.println("Finished Test " + setup.name);
            results.add(result);

        }

        //////////////////////////
        /// Create output file

        /// Write a header to the file
        StringBuilder str = new StringBuilder();
        str.append("Name");
        for (int size : sizes) {
            str.append(", Size of ");
            str.append(size);
        }
        str.append('\n');

        /// Write each result to a row
        for (TestResult result : results) {
            str.append(result.setup.name);
            for (int i = 0; i < result.dataSize.size(); i++) {
                str.append(',');
                str.append(result.averageDuration.get(i));
            }
            str.append('\n');
            System.out.println("Result for " + result.dataSize + ": " + result.setup.name + ": " + result.averageDuration + "ns");
        }

        /// Write the CSV to a file
        try {
            Path target = Path.of("./out.csv");
            /// Delete existing file
            Files.delete(target);

            /// Write all text to file and close
            Files.writeString(target, str.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<TestSetup> getTests() {
        /// Short names for method references + data for generating test variants
        PivotPicker middlePiv = Quick::alwaysPickMiddle;

        // Pivot Pickers
        PivotPicker leftPiv = Quick::alwaysPickLeftmost;
        PivotPicker rightPiv = Quick::alwaysPickRightmost;
        PivotPicker motPiv = Quick::medianOfThree;
        PivotPicker[] pivots = {leftPiv, rightPiv, motPiv};
        String[] pivotNames = {"Always Pick Leftmost", "Always Pick Rightmost", "Median Of Three"};

        // Partitioning methods
        Partitioner jonPart = (array, left, right, pivot) -> partition_Jon(array, left, right, pivot);
        Partitioner lomutoPart = Quick::partition_Lomuto;
        Partitioner hoarePart = Quick::partition_Hoare;
        Partitioner[] partitioners = {lomutoPart, hoarePart};
        String[] partNames = {"lomuto", "hoare"};

        // Subsort methods and sizes
        Subsort insertionSub = Quick::insertionSort;
        Subsort[] subsorts = {insertionSub};
        String[] subsortNames = {"insertion"};
        List<Integer> sizes = new ArrayList<>();
        for (int i = 2; i < 128; i = 1 + (int) (i * 1.15)) {
            sizes.add(i);
        }
        Integer[] subsortSizes = sizes.toArray(new Integer[sizes.size()]);

        // Data generators
        Generator randomGen = (size) -> {
            return generateArray(100, 999, size);
        };
        Generator sortedGen = (size) -> {
            int[] arr = new int[size];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = i;
            }
            return arr;
        };
        Generator reversedGen = (size) -> {
            int[] arr = new int[size];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = arr.length - i;
            }
            return arr;
        };
        Generator[] generators = {sortedGen, reversedGen,
                (size) -> {
                    return generateArray(10000, 99999, size, 0xDEADBEEF);
                },
                (size) -> {
                    return generateArray(10000, 99999, size, 0xCAFEBABE);
                },
                (size) -> {
                    return generateArray(10000, 99999, size, 0xBAADF00D);
                },

        };
        String[] generatorNames = {"sorted", "reversed",
                "Sequence:0xDEADBEEF",
                "Sequence:0xCAFEBABE",
                "Sequence:0xBAADF00D",
        };

        // Default test data
        TestSetup defaultTest = new TestSetup("Default", middlePiv, jonPart, 0, null, randomGen);

        List<TestSetup> tests = new ArrayList<>();
        tests.add(defaultTest);
        List<TestSetup> temp = new ArrayList<>();

        // Create variations of the test with different pivot picking methods
        for (TestSetup setup : tests) {
            temp.add(setup);

            for (int i = 0; i < pivots.length; i++) {
                TestSetup variant = new TestSetup(setup, pivotNames[i]);
                variant.pivp = pivots[i];
                temp.add(variant);
            }
        }

        { // Block for creating generator (dataset) variants
            // Swap arrays (don't want to add to a collection we're iterating)
            tests = temp;
            temp = new ArrayList<>();

            // Create variations of the test with different data generation methods
            for (TestSetup setup : tests) {
                temp.add(setup);

                for (int i = 0; i < generators.length; i++) {
                    TestSetup variant = new TestSetup(setup, setup.name + " on " + generatorNames[i] + " data");
                    variant.gen = generators[i];
                    temp.add(variant);
                }
            }
        }

        {  // Block for creating partition method variants

            // Swap arrays (don't want to add to a collection we're iterating)
            tests = temp;
            temp = new ArrayList<>();

            // Create variations of the test with different partitioning methods
            for (TestSetup setup : tests) {
                temp.add(setup);

                for (int i = 0; i < partitioners.length; i++) {
                    TestSetup variant = new TestSetup(setup, setup.name + "+" + partNames[i]);
                    variant.part = partitioners[i];
                    temp.add(variant);
                }
            }
        }

        { // Block for creating subsort variants
            // Swap arrays (don't want to add to a collection we're iterating)
            tests = temp;
            temp = new ArrayList<>();

            // Create variations of the test with different subsort methods and thresholds
            for (TestSetup setup : tests) {
                temp.add(setup);

                for (int i = 0; i < subsorts.length; i++) {
                    // And also one version for each size!
                    for (int k = 0; k < subsortSizes.length; k++) {
                        TestSetup variant = new TestSetup(setup, setup.name + "+" + subsortNames[i] + " below " + subsortSizes[k]);
                        variant.sst = subsortSizes[k];
                        variant.ssort = subsorts[i];
                        temp.add(variant);
                    }
                }
            }
        }


        // Return completed variation list
        return temp;
    }


    /**
     * Helper function @param array
     */
    private static void printArray(int[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i]);
            System.out.print(", ");
        }

        System.out.println("");
    }

    /**
     * Pivot function that always picks middlemost item as pivot
     */
    public static int alwaysPickMiddle(int[] arr, int left, int right) {
        return (left + right) / 2;
    }

    /**
     * Pivot function that always picks the leftmost item as pivot
     */
    public static int alwaysPickLeftmost(int[] arr, int left, int right) {
        return left;
    }

    /**
     * Pivot function that always picks the leftmost item as pivot
     */
    public static int alwaysPickRightmost(int[] arr, int left, int right) {
        return right;
    }

    /**
     * Pivot function that picks the median between left/right/middle
     */
    public static int medianOfThree(int[] arr, int left, int right) {
        int mid = (right + left) / 2;

        if (arr[right] < arr[left]) {
            swap(arr, left, right);
        }
        if (arr[mid] < arr[left]) {
            swap(arr, mid, left);
        }
        if (arr[right] < arr[mid]) {
            swap(arr, right, mid);
        }

        return mid;
    }

    /**
     * Insertion sort subsort
     */
    public static void insertionSort(int[] arr, int left, int right) {

        for (int i = left + 1; i <= right; i++) {
            for (int k = i; k > left; k--) {
                if (arr[k] < arr[k - 1]) {
                    int temp = arr[k];
                    arr[k] = arr[k - 1];
                    arr[k - 1] = temp;
                }
            }

        }
    }

    /**
     * Swap helper
     */
    public static void swap(int[] arr, int a, int b) {
        int temp = arr[a];
        arr[a] = arr[b];
        arr[b] = temp;
    }

    /**
     * Entry into quicksort
     *
     * @param array            Array to sort
     * @param pivp             Function to pick pivot index for sort
     * @param subSortThreshold Max size before switching to a different sorting algorithm
     * @param subsort          Secondary sorting algorithm to switch to under threshold
     */
    public static void sort(int[] array,
                            PivotPicker pivp,
                            Partitioner part,
                            int subSortThreshold,
                            Subsort subsort) {
        sort(array, 0, array.length - 1, pivp, part, subSortThreshold, subsort);
    }

    /**
     * Actual quicksort
     *
     * @param left             low index of region to sort
     * @param right            high index of region to sort
     * @param array            Array to sort
     * @param pivp             Function to pick pivot index for sort
     * @param subSortThreshold Max size before switching to a different sorting algorithm
     * @param subsort          Secondary sorting algorithm to switch to under threshold
     */
    public static void sort(int[] array, int left, int right,
                            PivotPicker pivp,
                            Partitioner part,
                            int subSortThreshold,
                            Subsort subsort) {
        // Span length of elements to sort
        int span = right - left;

        // Base cases

        // Span of one element, trivially sorted
        if (span < 1) {
            return;
        }
        // Span of two elements, may need one swap
        if (span < 2) {
            if (array[left] > array[right]) {
                swap(array, left, right);
            }
            return;
        }

        /// Based on span, pick using recursive quicksort or using secondary sorting algorithm
        if (span > subSortThreshold || subsort == null) {
            // use quicksort if over threshold or no secondary
            int pivot = pivp.getPivot(array, left, right);


            int p = part.partition(array, left, right, pivot);
            // Recurse down left/right sides with quicksort
            sort(array, left, p - 1, pivp, part, subSortThreshold, subsort);
            sort(array, p + 1, right, pivp, part, subSortThreshold, subsort);
        } else {

            //System.out.println("Using subsort: " + left + " - " + right);
            // use provided subsort and call this span finished
            subsort.sort(array, left, right);

        }


    }

    /**
     * double-sided partition method I wrote
     *
     * @param array array to partition
     * @param left  low index of region to partition
     * @param right high index of region to partition
     * @param pivot index of pivot element
     * @return index where pivot was placed after partitioning
     */
    public static int partition_Jon(int[] array, int left, int right, int pivot) {
        // left pointer/right pointer values that are walked towards eachother
        int lp = left;
        int rp = right;
        // pivot value
        int pv = array[pivot];

        /// Loop until break
        while (true) {

            /// Walk LP rightward, looking for first value greater than pivot value
            while (array[lp] <= pv && lp != rp) {
                lp++;
            }
            if (lp == rp) {
                break;
            }

            /// Walk RP leftward, looking for first value less than pivot value
            while (array[rp] >= pv && lp != rp) {
                rp--;
            }
            if (lp == rp) {
                break;
            }

            // Swap values to correct sides
            swap(array, lp, rp);
        }
        // Find correct index to swap pivot into.
        int swapIndex = (pivot < lp)
                // If pivot is to the left, use the index of lower value
                ? ((array[lp] > pv) ? lp - 1 : lp)
                // If pivot is to the right, use the index of higher value
                : ((array[rp] < pv) ? rp + 1 : rp);

        // Swap elements
        swap(array, pivot, swapIndex);

        // Return index where pivot was placed
        return swapIndex;
    }

    public static int partition_Lomuto(int[] array, int left, int right, int pivot) {
        // Required: Pivot must be placed in leftmost position of subregion
        swap(array, left, pivot);

        int p = array[left];
        int s = left;

        for (int i = left + 1; i <= right; i++) {
            if (array[i] < p) {
                s = s + 1;
                swap(array, s, i);
            }
        }
        swap(array, left, s);
        return s;
    }

    public static int partition_Hoare(int[] array, int left, int right, int pivot) {
        // Required: Pivot must be placed in leftmost position of subregion
        swap(array, left, pivot);

        int p = array[left];
        int i = left;
        int j = right + 1;

        while (true) {

            while (true) {
                i = i + 1;
                if (i >= array.length) {
                    i = array.length - 1;
                    break;
                }
                if (array[i] >= p) {
                    break;
                }
            }
            while (true) {
                j = j - 1;
                if (j <= -1) {
                    i = 0;
                    break;
                }
                if (array[j] <= p) {
                    break;
                }
            }
            swap(array, i, j);

            if (i >= j) {
                break;
            }
        }

        swap(array, i, j);
        swap(array, left, j);

        return j;
    }

    /**
     * Generate an int[] with size elements in range [min, max)
     */
    public static int[] generateArray(int min, int max, int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = min + (int) (Math.random() * (max - min));
        }
        return arr;
    }

    /**
     * Generate an int[] with size elements in range [min, max) using the given seed
     */
    public static int[] generateArray(int min, int max, int size, long seed) {
        int[] arr = new int[size];
        Random rand = new Random(seed);
        for (int i = 0; i < size; i++) {
            arr[i] = min + (int) (rand.nextInt(max - min));
        }
        return arr;
    }

}
