/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists;

import java.util.*;
import java.io.*;
// the core Glazed Lists packages
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.matchers.ThreadedMatcherEditor;

/**
 * Utility class for analyzing the performance of the TextFilterList.
 *
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
public class TextFilterPerformance {

    /**
     * Execute a performance test that is specified on the command line.
     */
    public static void main(String[] args) throws Exception {
        if(args.length != 1) {
            System.out.println("Usage: TextFilterPerformance <testfile>");
            System.out.println("");
            System.out.println("<testfile> is a file in the following format:");
            System.out.println(" filter section:   (<filterstring><CRLF><expect><CRLF>)*");
            System.out.println(" separator:        <CRLF>");
            System.out.println(" elements section: ((<element string><CRLF>)*)<CRLF>*");
            System.out.println("");
            System.out.println("<CRLF> is a newline character");
            System.out.println("<filterstring> is a search string of space separated tokens");
            System.out.println("<expect> is an integer, the number of elements to match the filterstring");
            System.out.println("<element string> is a string component of an element");
            return;
        }
        
        // start reading
        System.out.print("Reading input...");
        BufferedReader in = new BufferedReader(new FileReader(args[0]));
        String line = "";
        
        // read the filter strings
        List testFilters = new ArrayList();
        List testHitCounts = new ArrayList();
        while(!(line = in.readLine()).equals("")) {
            testFilters.add(line);
            testHitCounts.add(new Integer(in.readLine()));
        }
        
        // read the input texts
        List elements = new ArrayList();
        List currentElement = new ArrayList();
        elements.add(currentElement);
        while((line = in.readLine()) != null) {
            if(line.equals("")) {
                currentElement = new ArrayList();
                elements.add(currentElement);
            } else {
                currentElement.add(line);
            }
        }
        
        // we're done reading
        System.out.println(" done. " + elements.size() + " elements");
        in.close();

        // summarize what we've read
        for(int f = 0; f < testFilters.size(); f++) {
            System.out.println("Filter " + f + ": " + testFilters.get(f) + ", expect: " + testHitCounts.get(f) + " matches");
        }
        
        // prepare the filter list
        BasicEventList unfiltered = new BasicEventList();
        unfiltered.addAll(elements);
        TextMatcherEditor textMatcherEditor = new TextMatcherEditor(new CollectionTextFilterator());
        FilterList filtered = new FilterList(unfiltered, textMatcherEditor);
        
        // track time
        long startTime = 0;
        long finishTime = 0;


        System.out.println("");
        System.out.println("Full Filter");
        long fullFilterTime = 0;
        // perform the filters
        for(int i = 0; i < testFilters.size(); i++) {
            String filter = (String)testFilters.get(i);
            System.out.print("Filtering " + i + ", \"" + filter + "\"...");

            int expectedResult = ((Integer)testHitCounts.get(i)).intValue();
            startTime = System.currentTimeMillis();
            textMatcherEditor.setFilterText(filter.split("[ \t]"));
            finishTime = System.currentTimeMillis();
            long totalFilteringTime = (finishTime - startTime);
            fullFilterTime += totalFilteringTime;

            if(filtered.size() != expectedResult) {
                System.out.println("expected size " + expectedResult + " != actual size " + filtered.size() + " for filter " + filter);
                for(int j = 0; j < filtered.size(); j++) {
                    System.out.println("" + j + ": " + filtered.get(j));
                }
                return;
            }
            System.out.println(" done. Total: " + totalFilteringTime);
        }
        System.out.println("Total: " + fullFilterTime);


        System.out.println("");
        System.out.println("Character-by-character Filter (no delays with TextMatcherEditor)");
        fullFilterTime = 0;
        // perform the filters 1 char at a time (to simulate the user typing)
        for(int i = 0; i < testFilters.size(); i++) {
            String filter = (String)testFilters.get(i);
            long totalFilteringTime = 0;
            long totalUnfilteringTime = 0;
            System.out.print("Filtering " + i + ", \"" + filter + "\" by character...");

            // simulate filter by the keystroke
            for (int j = 1; j <= filter.length(); j++) {
                String subFilter = filter.substring(0, j);

                startTime = System.currentTimeMillis();
                textMatcherEditor.setFilterText(subFilter.split("[ \t]"));
                finishTime = System.currentTimeMillis();

                totalFilteringTime += (finishTime - startTime);
            }

            // check the filtered result
            int expectedResult = ((Integer)testHitCounts.get(i)).intValue();
            if(filtered.size() != expectedResult) {
                System.out.println("expected size " + expectedResult + " != actual size " + filtered.size() + " for filter " + filter);
                for(int j = 0; j < filtered.size(); j++) {
                    System.out.println("" + j + ": " + filtered.get(j));
                }
                return;
            }

            // simulate unfiltering by the keystroke
            for (int j = 1; j <= filter.length(); j++) {
                String subFilter = filter.substring(0, filter.length() - j);
                startTime = System.currentTimeMillis();
                textMatcherEditor.setFilterText(subFilter.split("[ \t]"));
                finishTime = System.currentTimeMillis();

                totalUnfilteringTime += (finishTime - startTime);
            }

            System.out.println(" done. Filter: " + totalFilteringTime + ", Unfilter: " + totalUnfilteringTime + ", Total: " + (totalFilteringTime + totalUnfilteringTime));
            fullFilterTime += (totalFilteringTime + totalUnfilteringTime);
        }
        System.out.println("Total: " + fullFilterTime);


        System.out.println("");
        System.out.println("Simulated Typing Character-by-character Filter (delays with TextMatcherEditor)");
        fullFilterTime = 0;
        // perform the filters 1 char at a time (to simulate the user typing)
        for(int i = 0; i < testFilters.size(); i++) {
            String filter = (String)testFilters.get(i);
            long totalFilteringTime = 0;
            long totalUnfilteringTime = 0;
            System.out.print("Filtering " + i + ", \"" + filter + "\" by character...");

            // simulate filter by the keystroke
            for (int j = 1; j <= filter.length(); j++) {
                String subFilter = filter.substring(0, j);

                startTime = System.currentTimeMillis();
                textMatcherEditor.setFilterText(subFilter.split("[ \t]"));
                // simulate a small delay between keystrokes, as normally occurs when one types
                if (j <= filter.length())
                    Thread.sleep(100);
                finishTime = System.currentTimeMillis();

                totalFilteringTime += (finishTime - startTime);
            }
            fullFilterTime += totalFilteringTime;

            // check the filtered result
            int expectedResult = ((Integer)testHitCounts.get(i)).intValue();
            if(filtered.size() != expectedResult) {
                System.out.println("expected size " + expectedResult + " != actual size " + filtered.size() + " for filter " + filter);
                for(int j = 0; j < filtered.size(); j++) {
                    System.out.println("" + j + ": " + filtered.get(j));
                }
                return;
            }

            // simulate unfiltering by the keystroke
            for (int j = 1; j <= filter.length(); j++) {
                String subFilter = filter.substring(0, filter.length() - j);
                startTime = System.currentTimeMillis();
                textMatcherEditor.setFilterText(subFilter.split("[ \t]"));
                // simulate a small delay between keystrokes, as normally occurs when one types
                if (j <= filter.length())
                    Thread.sleep(100);
                finishTime = System.currentTimeMillis();

                totalUnfilteringTime += (finishTime - startTime);
            }
            fullFilterTime += totalUnfilteringTime;

            System.out.println(" done. Filter: " + totalFilteringTime + ", Unfilter: " + totalUnfilteringTime + ", Total: " + (totalFilteringTime + totalUnfilteringTime));
        }
        System.out.println("Total: " + fullFilterTime);


        // attach a ThreadedMatcherEditor to the FilterList rather than a regular TextMatcherEditor
        textMatcherEditor = new TextMatcherEditor(new CollectionTextFilterator());
        MatcherEditor bufferedMatcherEditor = new ThreadedMatcherEditor(textMatcherEditor);
        filtered = new FilterList(unfiltered, bufferedMatcherEditor);

        System.out.println("");
        System.out.println("Simulated Typing Character-by-character Filter (delays with ThreadedMatcherEditor)");
        fullFilterTime = 0;
        // perform the filters 1 char at a time (to simulate the user typing)
        for(int i = 0; i < testFilters.size(); i++) {
            String filter = (String)testFilters.get(i);
            long totalFilteringTime = 0;
            long totalUnfilteringTime = 0;
            System.out.print("Filtering " + i + ", \"" + filter + "\" by character with buffering...");

            startTime = System.currentTimeMillis();
            // simulate filter by the keystroke
            for (int j = 1; j <= filter.length(); j++) {
                String subFilter = filter.substring(0, j);
                textMatcherEditor.setFilterText(subFilter.split("[ \t]"));

                // simulate a small delay between keystrokes, as normally occurs when one types
                if (j <= filter.length())
                    Thread.sleep(100);
            }

            // poll until the filter is fully applied
            int expectedResult = ((Integer)testHitCounts.get(i)).intValue();
            long pollingStartTime = System.currentTimeMillis();
            while (filtered.size() != expectedResult) {
                if (System.currentTimeMillis() - pollingStartTime > 10000) {
                    System.out.println("Stopping performance test because buffered Character-by-character Filter failed to be applied within 10 seconds.");
                    System.exit(-1);
                }
                Thread.sleep(100);
            }

            finishTime = System.currentTimeMillis();
            totalFilteringTime += (finishTime - startTime);
            fullFilterTime += totalFilteringTime;

            // simulate unfiltering by the keystroke
            startTime = System.currentTimeMillis();
            for (int j = 1; j <= filter.length(); j++) {
                String subFilter = filter.substring(0, filter.length() - j);
                textMatcherEditor.setFilterText(subFilter.split("[ \t]"));

                // simulate a small delay between keystrokes, as normally occurs when one types
                if (j <= filter.length())
                    Thread.sleep(100);
            }

            // poll until the filter is fully applied
            pollingStartTime = System.currentTimeMillis();
            while (filtered.size() != unfiltered.size()) {
                if (System.currentTimeMillis() - pollingStartTime > 10000) {
                    System.out.println("Stopping performance test because buffered Character-by-character Filter failed to be applied within 10 seconds.");
                    System.exit(-1);
                }
                Thread.sleep(100);
            }

            finishTime = System.currentTimeMillis();
            totalUnfilteringTime += (finishTime - startTime);
            fullFilterTime += totalUnfilteringTime;

            System.out.println(" done. Filter: " + totalFilteringTime + ", Unfilter: " + totalUnfilteringTime + ", Total: " + (totalFilteringTime + totalUnfilteringTime));
        }

        System.out.println("Total: " + fullFilterTime);
    }
    
    /**
     * A TextFilterator for collections of Strings.
     */
    static class CollectionTextFilterator implements TextFilterator<Collection<String>> {
        public void getFilterStrings(List<String> baseList, Collection<String> element) {
            baseList.addAll(element);
        }
    }
}