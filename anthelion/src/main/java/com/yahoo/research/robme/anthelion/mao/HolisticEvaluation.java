package com.yahoo.research.robme.anthelion.mao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import moa.classifiers.Classifier;
import moa.core.InstancesHeader;
import moa.streams.ArffFileStream;
import weka.core.Instance;

import com.yahoo.research.robme.anthelion.models.ClassifierEnum;

/**
 * Class to evaluate the performance of a classifier based on a datastream
 * (.arff). This class does not simulate a crawler and does not include a bandit
 * based selection.
 * 
 * @author Robert Meusel (robert@dwslab.de)
 * 
 */
public class HolisticEvaluation {

	public static void main(String[] args) {
		if (args == null || args.length < 8) {
			System.out
					.println("Usage: FilterEvalution <ARFFFILE> <CLASSINDEX> <CLASSIFIER> <CACHESIZE> <CACHEREFILLRATIO> <RESULTINTERVAL>");
			System.exit(0);
		} else {
			classifyStream(args[0], Integer.parseInt(args[1]),
					ClassifierEnum.getClassifier(args[2]),
					Integer.parseInt(args[3]), Integer.parseInt(args[4]),
					Integer.parseInt(args[5]));
		}
	}

	/**
	 * Classifies data from a stream using a {@link ArffFileStream} to read
	 * files and classifies them by test-and-train
	 * 
	 * @param file
	 *            the file (.arff) to process
	 * @param classindex
	 *            the index of the class in the arff file
	 * @param cn
	 *            the classifier to use (see {@link ClassifierEnum})
	 * @param cacheSize
	 *            the size of items to load into cache. Items in cache will be
	 *            shuffled befor processed.
	 * @param cacheRatioForRefill
	 *            ratio x when the cache is refilled. If cache size is lower
	 *            than 1/x of cachesize the cache will be reload.
	 * @param resultInterval
	 *            reporting interval
	 */
	private static void classifyStream(String file, int classindex,
			Classifier learner, int cacheSize, int cacheRatioForRefill,
			int resultInterval) {

		// create the stream to read the data
		ArffFileStream stream = new ArffFileStream(file, classindex);

		InstancesHeader header = stream.getHeader();
		learner.setModelContext(header);
		learner.prepareForUse();

		// set variables
		int maxItemSize = cacheSize;
		int minItemSizeRatio = cacheRatioForRefill;
		int sampleNum = 0;
		int correctClassified = 0;
		double accuracy = 0.0;

		List<Instance> arffItems = new ArrayList<Instance>();

		System.out.println("SAMPLESIZE	ACCURACY");

		boolean run = true;
		while (run) {

			while (arffItems.size() > ((int) maxItemSize / minItemSizeRatio)
					|| !stream.hasMoreInstances()) {

				Instance instance = arffItems.remove(0);

				if (learner.correctlyClassifies(instance)) {
					correctClassified++;
				}
				learner.trainOnInstance(instance);
				sampleNum++;
				if (sampleNum % resultInterval == 0) {
					accuracy = 100.0 * (double) correctClassified
							/ (double) sampleNum;
					System.out.println(sampleNum + "\t" + accuracy);
				}

			}
			while (stream.hasMoreInstances() && arffItems.size() < maxItemSize) {
				arffItems.add(stream.nextInstance());
			}
			// shake it baby!
			Collections.shuffle(arffItems);

			if (!stream.hasMoreInstances() && arffItems.size() == 0) {
				accuracy = 100.0 * (double) correctClassified
						/ (double) sampleNum;
				// all items processed
				run = false;
			}

		}

	}
}
