package edu.arizona.cs;

import org.apache.log4j.BasicConfigurator;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.util.CoreMap;
@SuppressWarnings("deprecation")


public class App {
	
	public static void main(String args[]) throws IOException {
		BasicConfigurator.configure();
		
		File wikiDir = new File(args[0]);
		File[] files = wikiDir.listFiles();
		StandardAnalyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config =new IndexWriterConfig(analyzer);
		Directory index = null;
		IndexWriter writer = null;
		try {
			index = Indexer("watsonIndex");
			writer = new IndexWriter(index,config);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to create index and/or writer");
		}
		
		
		//Uncomment to create the index again (will take 2+ hours)
		//populateIndex(files,writer);
		//writer.commit();
		
		System.out.println("Enter a query");
		Scanner queryScan = new Scanner(System.in);
		String query = queryScan.nextLine();
		String preProcQuery = processText(query);
		try {
			queryData(index,analyzer,preProcQuery);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		//Uncomment to evaluate the code over the 100 given questions
		/*try {
			evaluate(index,analyzer);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
	}
	
	/**
	 * Creates the FSDirectory for the watson index.
	 * @param indexPath
	 * @return
	 * @throws IOException
	 */
	public static Directory Indexer(String indexPath) throws IOException {
		String indexDirectory = "watsonIndex";
		Directory index = FSDirectory.open(new File(indexDirectory).toPath());
		return index;	
		   }
	
	
	/**
	 * Calls the createDocsFromFiles method for each file in the directory 'files'.
	 * @param files
	 * @param writer
	 * @throws IOException
	 */
	public static void populateIndex(File[] files, IndexWriter writer) throws IOException {
		for(int i=0;i<files.length;i++) {
			System.out.println(files[i]);
			createDocsFromFiles(files[i],writer);
			
		}
	}
	
	
	/**
	 * Takes a single wikipedia file and parses it into individual documents to index,
	 * then calls addDoc to index the file.
	 * @param file the file to 
	 * @param writer
	 * @throws IOException
	 */
	public static void createDocsFromFiles(File file,IndexWriter writer) throws IOException{
		Scanner fileReader = null;
		try {
			fileReader = new Scanner(file);
		} catch (FileNotFoundException e) {
			System.out.println("Failed to read file");
		}
		String contentString = "";
		String titleString = "";
		while(fileReader.hasNext()) {
			String nextString = fileReader.next();
			
			if(nextString.startsWith("[[") && (!(nextString.startsWith("[[File:")||nextString.startsWith("[[Media:")||nextString.startsWith("[[Image:")))) {
				if(!(titleString.equals("") && (contentString.equals("")))) {
					addDoc(writer,titleString,contentString);
					
				}
			
				titleString = nextString;
				contentString ="";
				
					if(!titleString.contains("]]")) {
						String midTitle = "";
						
						while(!midTitle.contains("]]")) {
							if(fileReader.hasNext()) {
								midTitle = fileReader.next();
								titleString = titleString+" "+midTitle;
							}
						}	
					}
				}
			else {
				if(fileReader.hasNext())
					contentString += nextString+" ";
			}
			}
	}
	/**
	 * Adds the given document body and title to the index
	 * Calls processText on the body in order to preprocess text (eg lemmatize and remove unwanted characters)
	 * @param writer index writer
	 * @param title the given title of the wikipedia doc to index
	 * @param body the given body of the wikipedia doc to inedex
	 * @throws IOException
	 */
	
	public static void addDoc(IndexWriter writer,String title, String body) throws IOException {
		//System.out.println("Title: "+title);
		//System.out.println("Body :"+body);
		body = processText(body);
		Document doc = new Document();
		doc.add(new TextField("body",body,Field.Store.YES));
		doc.add(new StringField("title",title,Field.Store.YES));
		writer.addDocument(doc);
		
		
	}
	
	/**
	 * does preprocessing on some string text. Removes punctuation, and lemmatizes text.
	 * @param textBody
	 * @return
	 */
	public static String processText(String textBody) {
		textBody = textBody.replaceAll("[^a-zA-z 0-9*]", " ");
		String newString = "";
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		Annotation doc = pipeline.process(textBody);
		List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap sentence:sentences) {
			for(CoreLabel token: sentence.get(TokensAnnotation.class)) {
				newString = newString + token.get(LemmaAnnotation.class)+" ";
			}
		}
		System.out.println(newString);
		return textBody;
	}
		
	/**
	 * Takes in a query string s, queries the index on this string,
	 * prints out the top results.
	 * @param index
	 * @param analyzer
	 * @param s
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void queryData(Directory index, StandardAnalyzer analyzer, String s) throws IOException, ParseException {
		
		Query q = new QueryParser("body",analyzer).parse(s);
		
		int hits = 10;
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		//searcher.setSimilarity(new BM25Similarity());
		TopDocs docs = searcher.search(q, hits);
		ScoreDoc[] docsHit = docs.scoreDocs;
		
		for(int i=0;i<docsHit.length;i++) {
			int docId = docsHit[i].doc;
			Document d = searcher.doc(docId);
			if(i==0) {
				System.out.print("Answer: ");
			}
			System.out.println(d.get("title"));
			if(i==0) {
				System.out.println();
				System.out.println("Other matching wikipedia docs:");
			}
		}
		
	}
	
	/**
	 * Evaluates the answer to each of the 100 given queries
	 * calculates MRR and shows the total answered correctly at the end of the function
	 * @param index
	 * @param analyzer
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void evaluate(Directory index, StandardAnalyzer analyzer) throws IOException, ParseException {
		File questionsFile = new File("questions.txt");
		Scanner fileScan = new Scanner(questionsFile);
		float score = 0;
		int totalQueries = 0;
		int totalCorrect =0;
		int totalMatched=0;
		while(fileScan.hasNextLine()) {
			totalQueries++;
			String category = fileScan.nextLine();
			String question = fileScan.nextLine();
			String answer = fileScan.nextLine();
			String answer2 = "";
			if(answer.contains("|")) {
				System.out.println("Contains 2 answers");
				String[] answers = answer.split("\\|");
				answer = answers[0];
				answer2 = answers[1];
			}
			if(fileScan.hasNextLine()) {
				fileScan.nextLine();
			}
			//performs each query here
			Query q = new QueryParser("body",analyzer).parse(processText(category+" " +question));
			
			int hits = 10;
			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			//searcher.setSimilarity(new BooleanSimilarity());
			TopDocs docs = searcher.search(q, hits);
			ScoreDoc[] docsHit = docs.scoreDocs;
			
			for(int i=0;i<docsHit.length;i++) {
				int docId = docsHit[i].doc;
				Document d = searcher.doc(docId);
				//checks if the answer is in the documents returned
				if((d.get("title").equals("[["+answer+"]]"))||d.get("title").equals("[["+answer2+"]]")) {
					if(i==0) {
						System.out.println("totalCorrect++");
						totalCorrect++;
					}
					else {
						totalMatched++;
					}
					System.out.println("answer found at pos " +(i+1));
					float tempPos = i+1;
					float tempNumerator = 1;
					
					score+= tempNumerator/tempPos;
						
					System.out.println(score);
				}
				System.out.println(d.get("title"));
			}
			System.out.println(answer);
			System.out.println(answer2);
		}
		System.out.println("Total queries answered correctly: " + totalCorrect);
		System.out.println("Total queries with answer in top 10: "+ totalMatched );
		System.out.println("MRR Score: " + score/totalQueries);
	}
	
}
