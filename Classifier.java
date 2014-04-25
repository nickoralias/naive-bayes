import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.StringTokenizer;

public class Classifier {

	HashMap<String, HashMap<String, Integer>> countMap;
	HashMap<String, HashMap<String, Double>> probabilities;
	HashMap<String, Double> results;
	HashSet<String> words;
	HashSet<String> categories;
	HashSet<String> stopWords;
	int n;
	int numRight = 0;
	int numClassified = 0;

	public static void main(String[] args) throws Exception
	{
		Classifier classifier = new Classifier();
		classifier.read();
		System.out.printf("Overall accuracy: %d out of %d = %.2f", 
				classifier.numRight, classifier.numClassified, 
				(float)classifier.numRight/classifier.numClassified);
	}

	void read() throws Exception
	{
		String line = null, word = null, category = null, name = null;

		/* read in the stop words, create array */
			File file = new File("stopwords.txt");
		Scanner sc = new Scanner(file);
		stopWords = new HashSet<String>();
		while(sc.hasNextLine())
		{
			line = sc.nextLine();
			Scanner sw = new Scanner(line);
			while(sw.hasNext())
			{
				word = sw.next();
				stopWords.add(word);
			}
		}

		/* read in number of entries to use as training set */
		System.out.println("Enter number of entries to use as training set:");
		sc = new Scanner(System.in);
		n = sc.nextInt();

		/* read in the training set and initialize countMap */
		file = new File("corpus.txt");
		countMap = new HashMap<String, HashMap<String, Integer>>();
		words = new HashSet<String>();
		categories = new HashSet<String>();
		sc = new Scanner(file);
		boolean stop; //check if a word is a stop word
		int count = 0; //keep track of how many biographies have been read
		outerloop:
		while(sc.hasNextLine() && (count < n))
		{
			name = sc.nextLine().trim();
			if(name == null)
			{
				break;
			}
			while(name.isEmpty())
			{
				name = sc.nextLine().trim();
				if(name == null)
				{
					break outerloop;
				}
			}
			count++;
			category = sc.nextLine().trim();
			categories.add(category);
			if(!countMap.containsKey(category))
			{
				countMap.put(category, new HashMap<String, Integer>());
				countMap.get(category).put(null, 1);
			}
			else
			{
				countMap.get(category).put(null, 
						(countMap.get(category).get(null) + 1));
			}
			while(!(line = sc.nextLine().trim()).isEmpty())
			{
				line = line.toLowerCase().replaceAll("\\.", "").replaceAll("\\,", "");
				StringTokenizer st = new StringTokenizer(line);
				while(st.hasMoreTokens())
				{
					word = st.nextToken();
					if(stopWords.contains(word) || (word.length() <= 2))
					{
						stop = true;
					}
					else
					{
						stop = false;
					}
					if(!stop)
					{
						words.add(word);
						if(countMap.get(category).get(word) == null)
						{
							countMap.get(category).put(word, 1);
						}
						else
						{
							countMap.get(category).put(word, 
									(countMap.get(category).get(word) + 1));
						}
					}
				}
			}
		}

		/* calculate the probabilities */
		initProbabilities();

		/* read in the test data for classification */
		results = new HashMap<String, Double>();
		for(String c: categories)
		{
			results.put(c, probabilities.get(c).get(null));
		}
		count = 0;
		outerloop:
		while(sc.hasNextLine())
		{
			name = sc.nextLine().trim();
			if(name == null)
			{
				break;
			}
			while(name.isEmpty())
			{
				name = sc.nextLine().trim();
				if(name == null)
				{
					break outerloop;
				}
			}
			count++;
			category = sc.nextLine(); //compare to prediction later
			line = sc.nextLine().trim();
			while(!line.isEmpty())
			{
				line = line.toLowerCase().replaceAll("\\.", "").replaceAll("\\,", "");
				StringTokenizer st = new StringTokenizer(line);
				while(st.hasMoreTokens())
				{
					word = st.nextToken();
					if(stopWords.contains(word) || (word.length() <= 2) || (!words.contains(word)))
					{
						stop = true;
					}
					else
					{
						stop = false;
					}
					if(!stop)
					{
						for(String c: categories)
						{
							results.put(c, (results.get(c) + 
									probabilities.get(c).get(word)));
						}
					}
				}
				if(!sc.hasNextLine())
				{
					break;
				}
				line = sc.nextLine().trim();
			}
			printResult(name, category.trim());
			for(String c: categories)
			{
				results.put(c, probabilities.get(c).get(null));
			}
			if(!sc.hasNextLine())
			{
				break;
			}
		}
	}
	
	void printResult(String name, String category)
	{
		String prediction = null;
		double min = 0;
		for(String c: categories)
		{
			if(prediction == null)
			{
				prediction = c;
				min = results.get(c);
			}
			else if(results.get(c) < min)
			{
				prediction = c;
				min = results.get(c);
			}
		}
		recoverActual(min);
		if(prediction.equals(category))
		{
			numRight++;
			System.out.println(name + ".  Prediction: " + prediction + ".  Right.");
		}
		else
		{
			System.out.println(name + ".  Prediction: " + prediction + ".  Wrong.");
		}
		numClassified++;
		for(String c: categories)
		{
			System.out.printf("%s: %.2f     ", c, results.get(c));
		}
		System.out.println("\n");
	}
	
	void recoverActual(double min)
	{
		double s = 0.0;
		for(String c: categories)
		{
			if((results.get(c) - min) < 7)
			{
				s += Math.pow(2, (min - results.get(c)));
			}
		}
		for(String c: categories)
		{
			if((results.get(c) - min) < 7)
			{
				results.put(c, (Math.pow(2, (min - results.get(c)))/s));	
			}
			else
			{
				results.put(c, 0.0);
			}
		}
	}

	double calculateFreq(String c, String w)
	{
		if(!countMap.get(c).containsKey(w))
		{
			return 0;
		}
		else
		{
			if(w == null)
			{
				return (double)countMap.get(c).get(w)/n;
			}
			else
			{
				return (double)countMap.get(c).get(w)/countMap.get(c).get(null);
			}
		}
	}

	double calculateP(String category, String word)
	{
		double epsilon = 0.1;
		if(word == null)
		{
			return (calculateFreq(category, word) + epsilon)/(1 + ((categories.size()) * epsilon));
		}
		else
		{
			return (calculateFreq(category, word) + epsilon)/(1 + (2 * epsilon));
		}
	}

	double calculateL(String category, String word)
	{
		return -(Math.log(calculateP(category, word))/Math.log(2));
	}
	
	void initProbabilities()
	{
		probabilities = new HashMap<String, HashMap<String, Double>>();
		double probability;
		for(String category: categories)
		{
			probabilities.put(category, new HashMap<String, Double>());
			probability = calculateL(category, null);
			probabilities.get(category).put(null, probability);
			for(String word: words)
			{
				probability = calculateL(category, word);
				probabilities.get(category).put(word, probability);
			}
		}
	}

	void printProbabilities()
	{
		for(String category: categories)
		{
			System.out.print("category: " + category + ", probability: ");
			System.out.println(probabilities.get(category).get(null));
			for(String word: words)
			{
				System.out.print("word: " + word + ", probability: ");
				System.out.println(probabilities.get(category).get(word));
			}
		}
	}
}
