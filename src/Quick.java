import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Quick {
	/** Interface to pick a pivot for quicksort*/
	interface PivotPicker { int getPivot(int[] arr, int left, int right); }
	/** Interface to hold another sorting function 
	 * to use on runs of the array under a certain size*/
	interface Subsort { void sort(int[] arr, int left, int right); }
	/** Interface for generating test data. 
	 This way we can vary the test data per test */
	interface Generator { int[] generate(int size); }
	
	/** Data class for holding test configuration */
	static class TestSetup {
		public String name;
		public PivotPicker pp;
		public int sst;
		public Subsort ss;
		public Generator g;
		
		public TestSetup(String name, PivotPicker pp, int sst, Subsort ss) {
			this(name, pp, sst, ss, null);
		}
		public TestSetup(String name, PivotPicker pp, int sst, Subsort ss, Generator g) {
			this.name = name; this.pp = pp; this.sst = sst; this.ss = ss; this.g = g;
		}
	}
	
	/** Data class for holding test results  */
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
		
		/// Short names for method references
		PivotPicker leftPiv = Quick::alwaysPickLeftmost;
		PivotPicker mot = Quick::medianOfThree;
		
		Subsort insertion = Quick::insertionSort;
		Generator random = (size) -> { return generateArray(100, 999, size); };
		Generator sorted = (size) -> { 
			int[] arr = new int[size];
			for (int i = 0; i < arr.length; i++) {
				arr[i] = i;
			}
			return arr;
		};
		
		Generator reversed = (size) -> {
			int[] arr = new int[size];
			for (int i = 0; i < arr.length; i++) {
				arr[i] = arr.length - i;
			}
			return arr;
		};
		
		
		/// Test data, names, methods, and threshold for using a subsort like insertion sort.
		TestSetup[] tests = {
			// Name, pivotPicker, size for subsort, subsort algorithm
			new TestSetup("Default", null, 0, null),
			new TestSetup("Always pivot on left", leftPiv, 0, null),
			new TestSetup("Median of three", mot, 0, null),
			new TestSetup("Median of three+insertion under 32", mot, 32, insertion),
			new TestSetup("Median of three+insertion under 16", mot, 16, insertion),
			new TestSetup("Median of three+insertion under 8", mot, 8, insertion),
			new TestSetup("Always left+insertion under 32", leftPiv, 32, insertion),
			new TestSetup("Always left+insertion under 16", leftPiv, 16, insertion),
			new TestSetup("Always left+insertion under 8", leftPiv, 8, insertion),
			new TestSetup("Default+insertion under 8", null, 8, insertion),
			new TestSetup("Default+insertion under 16", null, 16, insertion),
			new TestSetup("Default+insertion under 32", null, 32, insertion),
			
			new TestSetup("Presorted+Default", null, 0, null, sorted),
			new TestSetup("Presorted+Always pivot on left", leftPiv, 0, null, sorted),
			new TestSetup("Presorted+Median of three", mot, 0, null, sorted),
			new TestSetup("Presorted+Median of three+insertion under 32", mot, 32, insertion, sorted),
			new TestSetup("Presorted+Median of three+insertion under 16", mot, 16, insertion, sorted),
			new TestSetup("Presorted+Median of three+insertion under 8", mot, 8, insertion, sorted),
			new TestSetup("Presorted+Always left+insertion under 32", leftPiv, 32, insertion, sorted),
			new TestSetup("Presorted+Always left+insertion under 16", leftPiv, 16, insertion, sorted),
			new TestSetup("Presorted+Always left+insertion under 8", leftPiv, 8, insertion, sorted),
			new TestSetup("Presorted+Default+insertion under 8", null, 8, insertion, sorted),
			new TestSetup("Presorted+Default+insertion under 16", null, 16, insertion, sorted),
			new TestSetup("Presorted+Default+insertion under 32", null, 32, insertion, sorted),
			
			new TestSetup("Reversed+Default", null, 0, null, reversed),
			new TestSetup("Reversed+Always pivot on left", leftPiv, 0, null, reversed),
			new TestSetup("Reversed+Median of three", mot, 0, null, reversed),
			new TestSetup("Reversed+Median of three+insertion under 32", mot, 32, insertion, reversed),
			new TestSetup("Reversed+Median of three+insertion under 16", mot, 16, insertion, reversed),
			new TestSetup("Reversed+Median of three+insertion under 8", mot, 8, insertion, reversed),
			new TestSetup("Reversed+Always left+insertion under 32", leftPiv, 32, insertion, reversed),
			new TestSetup("Reversed+Always left+insertion under 16", leftPiv, 16, insertion, reversed),
			new TestSetup("Reversed+Always left+insertion under 8", leftPiv, 8, insertion, reversed),
			new TestSetup("Reversed+Default+insertion under 8", null, 8, insertion, reversed),
			new TestSetup("Reversed+Default+insertion under 16", null, 16, insertion, reversed),
			new TestSetup("Reversed+Default+insertion under 32", null, 32, insertion, reversed),
		};
		
		/// Data array sizes to run
		int[] sizes = { 
			25, 100, 250, 500,
			// 1000, 2500, 
			
			// these big ones choke out the really bad sorts (always pivot on left/right)
			//5000, 10000, 15000, 20000 
		};
		List<TestResult> results = new ArrayList(tests.length);
		/// Number of reps for each test 
		int numIterations = 1000;
		
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
					int[] testArray = setup.g != null
							? setup.g.generate(dataSize)
							: generateArray(100, 999, dataSize);
					
					//System.out.println("\n\n\nGenerated new array");
					//printArray(testArray);
						
					// Run sort and only time the sort
					long start = System.nanoTime();
					sort(testArray, setup.pp, setup.sst, setup.ss);
					long end = System.nanoTime();

					long duration = end - start;
					totalDuration += duration;
					durations.add(duration);

					//printArray(testArray);
				}
				
				/// Record the result for the current size
				result.dataSize.add(dataSize);
				result.durations.add(durations);
				result.averageDuration.add(totalDuration / numIterations);
			}
			
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
			try {
				Files.delete(target);
			} catch (Exception e) {}
			
			/// Write all text to file and close
			Files.writeString(target, str.toString(), 
				StandardOpenOption.CREATE,
				StandardOpenOption.WRITE);
			
		} catch (Exception e) { throw new RuntimeException(e); }
		
	}

	/** Helper function @param array */
	private static void printArray(int[] array) {
		for (int i = 0; i < array.length; i++) {
			System.out.print(array[i]);
			System.out.print(", ");
		}
		
		System.out.println("");
	}
	
	/** Pivot function that always picks the leftmost item as pivot */
	public static int alwaysPickLeftmost(int arr[], int left, int right) { return left; }
	/** Pivot function that picks the median between left/right/middle */
	public static int medianOfThree(int[] arr, int left, int right) {
		int aval = arr[left];
		int bval = arr[right];

		int mid = (right + left) / 2;
		int mval = arr[mid];

		if (arr[right] < arr[left]) { swap(arr, left, right); }
		if (arr[mid] < arr[left]) { swap(arr, mid, left); }
		if (arr[right] < arr[mid]) { swap(arr, right, mid); }

		return mid;
	}
	
	/** Insertion sort subsort */
	public static void insertionSort(int[] arr, int left, int right) {
		
		for (int i = left + 1; i <= right; i++) {
			for (int k = i; k > left; k--) {
				if (arr[k] < arr[k-1]) {
					int temp = arr[k];
					arr[k] = arr[k-1];
					arr[k-1] = temp;
				}
			}
			
		}
	}
	
	/** Swap helper  */
	public static void swap(int[] arr, int a, int b) {
		int temp = arr[a];
		arr[a] = arr[b];
		arr[b] = temp;
	}
	
	/** Entry into quicksort
	 * @param array Array to sort 
	 * @param pivot Function to pick pivot index for sort 
	 * @param subSortThreshold Max size before switching to a different sorting algorithm
	 * @param subsort Secondary sorting algorithm to switch to under threshold */
	public static void sort(int[] array, 
			PivotPicker pivot, 
			int subSortThreshold, 
			Subsort subsort) {
		sort(array, 0, array.length-1, pivot, subSortThreshold, subsort);
	}
	/** Actual quicksort
	 * @param left low index of region to sort 
	 * @param right high index of region to sort 
	 * @param array Array to sort 
	 * @param pivot Function to pick pivot index for sort 
	 * @param subSortThreshold Max size before switching to a different sorting algorithm
	 * @param subsort Secondary sorting algorithm to switch to under threshold */
	public static void sort(int[] array, int left, int right,
			PivotPicker pp, 
			int subSortThreshold, 
			Subsort subsort) {
		// Span length of elements to sort
		int span = right - left;
		
		// Base cases
		
		// Span of one element, trivially sorted
		if (span < 1) { return; }
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
			int pivot = (pp != null)
					? pp.getPivot(array, left, right)
					: (left + right) / 2;
			
			
			int p = partition_Jon(array, left, right, pivot);
			// Recurse down left/right sides with quicksort
			sort(array, left, p-1, pp, subSortThreshold, subsort);
			sort(array, p+1, right, pp, subSortThreshold, subsort);
		} else {
			
			//System.out.println("Using subsort: " + left + " - " + right);
			// use provided subsort and call this span finished
			subsort.sort(array, left, right);;
			
		}
		
		
	}
	
	/** Provided example partition method. Doesn't work unless pivot is rightmost element. */
	private static int partition(int[] arr, int left, int right, int pivot) {
        int pv = arr[pivot];
        int i = (left - 1);

        for (int j = left; j < right; j++) {
            if (arr[j] <= pv) {
                i++;

                int swapTemp = arr[i];
                arr[i] = arr[j];
                arr[j] = swapTemp;
            }
        }

        int swapTemp = arr[i + 1];
        arr[i + 1] = arr[right];
        arr[right] = swapTemp;

        return i + 1;
    }
	
	/** double-sided partition method I wrote 
	 * @param array array to partition
	 * @param left low index of region to partition
	 * @param right high index of region to partition
	 * @param pivot index of pivot element 
	 * @return index where pivot was placed after partitioning */
	public static int partition_Jon(int[] array, int left, int right, int pivot) {
		// left pointer/right pointer values that are walked towards eachother 
		int lp = left;
		int rp = right;
		// pivot value 
		int pv = array[pivot];
		
		/// Loop until break 
		while (true) {
			
			/// Walk LP rightward, looking for first value greater than pivot value
			while (array[lp] <= pv && lp != rp) { lp++; }
			if (lp == rp) { break; }
			
			/// Walk RP leftward, looking for first value less than pivot value
			while (array[rp] >= pv && lp != rp) { rp--; }
			if (lp == rp) { break; }
			
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
	
	/** Generate an int[] with size elements in range [min, max) */
	public static int[] generateArray(int min, int max, int size) {
		int[] arr = new int[size];
		for (int i = 0; i < size; i++) {
			arr[i] = min + (int)(Math.random() * (max-min));
		}
		return arr;
	}
	
}
