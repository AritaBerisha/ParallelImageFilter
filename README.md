# Parallel Image Filter

This project aims to parallelize an image blurring algorithm in Java. The repository contains implementations of a sequential image filter and a parallelized version using Java's Executor Service framework.

### Files
- **ImageFilter.java**: Contains the original sequential image blurring code.
- **ParallelExecutorImageFilter.java**: Implements a parallelized version of the image blurring algorithm using the Executor Service framework.
- **TestImageFilter.java**: Includes test cases to compare the performance of the sequential and parallel implementations.
- **output**: Directory containing the results and comparisons between the sequential and parallel versions.

### Usage

To run the project:
1. Compile the Java files.
2. Run the `TestImageFilter.java` file to execute the tests and compare the sequential and parallel implementations.
3. Check the output directory for the results and performance comparisons.

### Results
The `output` directory contains the saved results and comparisons between the sequential and parallel versions. These results can be used to analyze the performance improvements achieved by parallelizing the image filter using the Executor Service framework.

### Notes
This project was created as a part of a university assignment.
Performance improvements and any observations made during the implementation process are documented in the output files.