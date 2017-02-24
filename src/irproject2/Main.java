/*
 * CSE 535 - Information Retrieval, SUNY Buffalo Fall - 2016
 * Project 2 - Implementation of TAAT/DAAT Algorithms, reading from Lucene Index
 * Siddharth Pateriya
 */ 
package irproject2;

/**
 *
 * @author Siddharth1
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import javax.swing.text.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.*;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class Main {
    //Global HashMap for storing dictionary terms and their postins list
    public static HashMap<String, LinkedList<Integer>> index = new HashMap<>();   
   //Static variable for counting comparisons during DAAT AND Operation
    public static int countand;
    /**
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        String path = args[0];                         //Accept arguments from commandline as IndexPath
        String ofile = args[1];                        //Output File name
        String ifile = args[2];                        //Input File Name
        FileSystem fs = FileSystems.getDefault();
        Path path1 = fs.getPath(path);
        Document doc = null;
        String[] abc = null;
      
        IndexReader reader = DirectoryReader.open(FSDirectory.open(path1));     //Load given index from path
        Collection<String> f = MultiFields.getIndexedFields(reader);            // Get Fields from the index
        f.remove("_version_");                                                 //Remove these fields since they do not contain terms
        f.remove("id");                                                 
        Fields fields = MultiFields.getFields(reader);                        
        Iterator<String> fit = fields.iterator();
     
        String lang = null;
        ArrayList<String> terms1 = new ArrayList<>();   
        
        //Creating the dictionary and postings HashMap
        for (String a : f) {                                                //Looping over all text_xx fields

            Terms terms = fields.terms(a);

            TermsEnum termsEnum = terms.iterator();             //Create iterator for all terms of field text_xx
            PostingsEnum myEnum = null;
            BytesRef term;
            while ((term = termsEnum.next()) != null) {

                myEnum = MultiFields.getTermDocsEnum(reader, a, term);  //Create a Postings iterator to retrieve all postings of a term

                LinkedList<Integer> post = new LinkedList<>();
              
                while (myEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
                     post.add(myEnum.docID());
                   
                }
                index.put(termsEnum.term().utf8ToString(), post);        //Put Term and Postings LinkedList in the map
               
            }      
        }

       String fileName = ifile;                                         
       //Set Writer to write output to output text file
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(ofile), "UTF-8" ));
        //Read Input File containing query terms
        FileInputStream is = new FileInputStream((fileName));

        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(fileName), StandardCharsets.UTF_8));
        //Read input file line-by-line
        String line = null;
        while ((line = br.readLine()) != null) {
         //Check if Line is not empty or contains only blank spaces/characters
          while (line.isEmpty() || line.trim().equals("") || line.trim().equals("\n") || "".equals(line)
|| line.equals("")){
               line = br.readLine();
           if (line == null)
           break;           
           }
           if (line == null)
               break;
                           
            line.replaceAll("\uFEFF", "");                  //Remove Byte Order Mark from the line
            String[] tokens = line.split(" ");
            for (int i = 0; i < tokens.length; i++) {
                tokens[i] = tokens[i].replace("\uFEFF", "");  //Remove Byte Order Mark from each query, if present
            }
             List<String> qterms = Arrays.asList(tokens);     //Create list of query terms of a line
            if (qterms.isEmpty()){continue;} 
            List<String> qt1 = new ArrayList();
            boolean rem = false;
            for (int i = 0;i<qterms.size();i++){
                rem = false;
            if (i==0)
            {qt1.add(qterms.get(i));}
                        else{
            for (int j = 0;j<qt1.size();j++){
            if (qt1.get(j).equals(qterms.get(i)))
            {rem = true;
            break;}
            }
            if (rem==true){continue;}
            else{
                    qt1.add(qterms.get(i));
                            }
            }
            
            }
           
            GetPostings(qterms,bw);                           //Get Postings Lists of query terms
            TaatAnd(qterms,bw,qt1);                               //Perform Term-At-A-Time AND
            TaatOr(qterms,bw,qt1);                                //Perform Term-At-A-Time OR
            DaatAnd(qterms,bw,qt1);                               //Perform Doc-At-A-Time AND
            DaatOr(qterms, bw,qt1);                               //Perform Doc-At-A-Time OR
            bw.flush();
        }
            bw.close();                                       //Close Buffered Writer
      
    }

    
    //Function for performing TermAtATime AND
    public static void TaatAnd(List<String>qt1 ,BufferedWriter bw,List<String> qterms) throws IOException {
        int counter = 0;
        boolean found = false;
        List<Integer> tr = new ArrayList();
        List<Integer> res = new ArrayList();
        LinkedList<Integer> tres = index.get(qterms.get(0));         //Store postings of first term in list
        tr.addAll(tres);                                             //Add it to intermediate list
        int count = 0;
        for (int i = 1; i < qterms.size(); i++) {                   //Loop over all query terms, starting from 2nd
            List<Integer> del = new ArrayList<>();
            List<Integer> nlist = index.get(qterms.get(i));        //Postings of current term
          
            int k = 0, j = 0;

            while (j < tr.size() && k < nlist.size()) {           //Loop until any one list reaches it's end
              
                count++;                                          //Increment counter for comparison
                if(tr.get(j).intValue() == nlist.get(k).intValue())                     // If both values are equal
                        {                                         // add result to intermediate and increment both pointers
                    res.add(tr.get(j).intValue());
                    j++;
                    k++;
                  
                } else if (tr.get(j).intValue() < nlist.get(k).intValue()) {          //If Current docID is greater than intermediate list's docID
                    j++;                                        //increment pointer of intermediate list
                    count++;    
                } else {
                    count++;
                    k++;                                        // Current docID is smaller, so increment it
                }
              
            }
            tr = res;                                           //Save intermediate result to TR, which will go back to while loop to be used again

        }
      
        //Write results to file
        
         bw.write("TaatAnd");
            bw.newLine();
            bw.write(qt1.toString().replaceAll("\\[|\\]|,", ""));
            bw.newLine();
            Collections.sort(tr);
        String tand;
        if (tr.isEmpty())
        { tand = "empty";}
        else{
        tand = tr.toString().replaceAll("\\[|\\]|,", "");}
            bw.write("Results: " + tand);
            bw.newLine();
            bw.write("Number of documents in results: " + tr.size());
            bw.newLine();
            bw.write("Number of comparisons: " + count);
            bw.newLine();
        
    }

    //Function to perform DocAtATime OR
    public static void DaatOr(List<String> qt1 ,BufferedWriter bw,List<String> qterms) throws IOException {
        int count = 0;
       
       // Create list of iterators for each of the query terms
        List<ListIterator<Integer>> pointers = new LinkedList<>();
        //Create a List of all Postings Lists, for all query terms passed
        List<LinkedList<Integer>> qlist = new LinkedList<>();

        for (int i = 0; i < qterms.size(); i++) {
            if (index.containsKey(qterms.get(i))) {
                qlist.add(index.get(qterms.get(i)));        //adding postings lists LinkedList to List of Postings
            }
        }
        List<Integer> result = new ArrayList<>();          //List to store the result
        if (!qlist.isEmpty()) {             

            for (LinkedList<Integer> post : qlist) {                    // Add iterators for each postings list to the list of iterators
                ListIterator<Integer> iterator = post.listIterator();
                pointers.add(iterator);
            }
        }
        int s = 0;
        while (!pointers.isEmpty()) {                               // Run until iterators list is empty
         // while (s<pointers.size()) {
            Integer mindoc = pointers.get(0).next();                //Set min docID as first docID of first term
            pointers.get(0).previous();                            //Set iterator back to first docID
            for (ListIterator<Integer> p : pointers) {             //Find minimum docID traversing through all lists
                
                Integer q = p.next();
                int qi = q.intValue();
                p.previous();
                //count++;
                if (q.compareTo(mindoc) < 0) {
                    mindoc = qi;                    
                }
            }

            result.add(mindoc);                             //Add min docID to results
            
           // for(int s = 0; s<pointers.size();s++){
            boolean rem = false;
            ListIterator<Integer> t = null;
                      
//while(p.hasNext())  {
          //while (s<pointers.size()){
          for ( s = 0; s<pointers.size();s++){              //Loop over all postings list iterators
             ListIterator<Integer> p = pointers.get(s);
             // s++;
           //  ListIterator<Integer> q = p;
          if (!p.hasNext()) {                               //if Postings list for term has reached end, remove it from List of iterators
              rem = true;
                   pointers.remove(p);
                 
                } else {
                  count++;int x = p.next();
                    //if ( p.next().equals(mindoc)) {
                    if (x==mindoc){
                    if (!p.hasNext()) {                 
                            pointers.remove(p);         //if Postings list for term has reached end, remove it from List of iterators
                          
                        rem = true;
                        }
                    } else {
                        p.previous();                   //Set pointer back to original value after check for last term failed
                     
                    }

                }
        
           }
           
      //  }
      //s++;
        }
    
    Collections.sort(result);                                   //Sort results and print to file
    bw.write("DaatOr");
    bw.newLine();
    bw.write(qt1.toString().replaceAll("\\[|\\]|,", ""));
    bw.newLine();
    String dor = result.toString().replaceAll("\\[|\\]|,", "");
    if (dor == ""){dor = "empty";}
    bw.write("Results: " + dor );
    bw.newLine();
    bw.write("Number of documents in results: " + result.size());
    bw.newLine();
    bw.write("Number of comparisons: " + count);
    bw.newLine();
               }
    
    //Function for DocAtATimeAND
    public static void DaatAnd(List<String> qt1 ,BufferedWriter bw,List<String> qterms ) throws IOException{
    countand = 0;
    
      List<ListIterator<Integer>> pointers = new LinkedList<>();                //Create list of iterators of each postings list
        List<LinkedList<Integer>> qlist = new LinkedList<>();                   //List of postings lists for each query term

        //Add all postings lists to qlist
        for (int i = 0; i < qterms.size(); i++) {
            if (index.containsKey(qterms.get(i))) {
                qlist.add(index.get(qterms.get(i)));

            }
        }
        
        List<Integer> result = new ArrayList<>();
        if (!qlist.isEmpty()) {
            //Add iterators for each postings list to Pointers
            for (LinkedList<Integer> post : qlist) {
                ListIterator<Integer> iterator = post.listIterator();
                pointers.add(iterator);
            }
        }
        //Run until any of the list reaches it's max
        while(listend(pointers))
        {//Check if all documents contain a term's docID
        if (docequals(pointers))
        {
        result.add(pointers.get(0).next());   //add if all documents contain the term
        pointers.get(0).previous();
        for (ListIterator<Integer> p : pointers)
        {
        p.next();                                           
        }        
        }
        else
        { Integer maxdoc = max(pointers);           //  If docequals is false, find max docID
        
	for(ListIterator<Integer> p: pointers){         
	if(p.hasNext()){								
	//countand++;
	if(p.next().compareTo(maxdoc) < 0){ 	 
	} else {
            p.previous();							
        }									
	 }
            }
	}	
        }
    //   System.out.println("DAAT AND");
      //  System.out.println(result.toString());
        //System.out.println(countand);
      //  Set<Integer> hs = new HashSet<>();
      //  hs.addAll(result);
      //  result.clear();
       // result.addAll(hs);
    Collections.sort(result);                           //Sort and write results to file
    bw.write("DaatAnd");
    bw.newLine();
    bw.write(qt1.toString().replaceAll("\\[|\\]|,", ""));
    bw.newLine();
    String dand = null;
    if (result.isEmpty()){
        dand = "empty";
    }
    else{
    dand = result.toString().replaceAll("\\[|\\]|,", "");}
    bw.write("Results: " + dand );
    bw.newLine();
    bw.write("Number of documents in results: " + result.size());
    bw.newLine();
    bw.write("Number of comparisons: " + countand);
    bw.newLine();
    }
            //Function to calculate max of all docIDs of all query terms
        static Integer max(List<ListIterator<Integer>> pointers){
	Integer max = pointers.get(0).next();
	pointers.get(0).previous();
	for(int i=0; i < pointers.size()-1; i++){
		countand++;
		Integer x,y;
		x = pointers.get(i).next();
		pointers.get(i).previous();
		y = pointers.get(i+1).next();
		pointers.get(i+1).previous();
		if(y.compareTo(x) > 0){
			max = y;
		}
	}		
	return max;
}
               //Check if a postings list has reached it's end
    static boolean listend(List<ListIterator<Integer>> pointers){	
	for(ListIterator<Integer> p: pointers){
		//countand++;
		if(p.hasNext() == false){
			return false;
		}
	}
	return true;
}
    //Check if a docID is present in all terms' postings lists
    static boolean docequals(List<ListIterator<Integer>> pointers){
	
	Integer val = pointers.get(0).next();
	pointers.get(0).previous();
	for(int i=1; i<pointers.size();  i++){
		Integer val1 = pointers.get(i).next();
		pointers.get(i).previous();
		//countand++;
		if (!val.equals(val1)){
			return false;
		}
	}
	return true;
}
    
    public static void TaatOr(List<String> qt1, BufferedWriter bw,List<String> qterms ) throws IOException {

  LinkedList<Integer> tres = index.get(qterms.get(0));              //Get postings list of the first term
            List<Integer> tr = new ArrayList<>();
            tr.addAll(tres);                                        //add it to intermediate result
            int count = 0;
            //System.out.println(tr.toString());

            boolean found1 = false, found2;
            for (int i = 1; i < qterms.size(); i++) {           //Loop over all query terms
                int j = 0, k = 0;
                List<Integer> nlist = index.get(qterms.get(i)); //Set new list as postings list of current term
                List<Integer> intresOR = new ArrayList<>();     //List for intermediate results
                while (j < tr.size() && k < nlist.size()) {    //Loop over old and new list until either one ends
                    count++;
                    if (tr.get(j).intValue() < nlist.get(k).intValue()) { //If old value less than new, increment pointer for old and add it to int result
                      intresOR.add(tr.get(j).intValue());
                        j++;                        
                    }
                    else if(tr.get(j).intValue() > nlist.get(k).intValue()){    //If new list value less than 
                    intresOR.add(nlist.get(k).intValue());                      //old value then add it to result and increment it's pointer
                        count++;
                        k++;
                    }
                    else{
                    count++;                                        //If both docIDs equal, add that docID and increment both pointers
                        intresOR.add(tr.get(j).intValue());
                        j++;
                        k++;
                    }
                  
               
                }
                 //Add remaining elements to results list
                if (j < tr.size()) {
                    while (j < tr.size()) {
                        intresOR.add(tr.get(j).intValue());
                        j++;
                    }
                }

                if (k < nlist.size()) {
                    while (k < nlist.size()) {
                        intresOR.add(nlist.get(k).intValue());
                        k++;
                    }
                }
                tr = intresOR;
            }
                
        //Print results to file
            bw.write("TaatOr");
            bw.newLine();
            bw.write(qt1.toString().replaceAll("\\[|\\]|,", ""));
            bw.newLine();
            Collections.sort(tr);
            String tor = tr.toString().replaceAll("\\[|\\]|,", "");
                     
            bw.write("Results: " + tor);
            bw.newLine();
            bw.write("Number of documents in results: " + tr.size());
            bw.newLine();
            bw.write("Number of comparisons: " + count);
            bw.newLine();

    }
    
    //Function to get Postings List of each query term
    public static void GetPostings(List<String> qterms, BufferedWriter bw ) throws IOException {
       
          for (int i = 0; i<qterms.size();i++){         //Loop over all query terms in a line
              String s = qterms.get(i);                         
                
                if (index.containsKey(s)){
                LinkedList<Integer> posting = index.get(s);//Get postings list of term
             //Write to file
                bw.write("GetPostings");
                bw.newLine();
                bw.write(s);
                bw.newLine();
                String p = posting.toString().replaceAll("\\[|\\]|,", "");
                bw.write("Postings list: " + p);
                bw.newLine();
                }
            }
        
    }
           
    }


//index.get(qterms[0]);
/**
 *
 * @param qlist
 * @param index
 * @return
 */
