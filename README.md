# Performium

A little Java project that uses Selenium to get performance metrics from a webpage. Runs on Chrome only

Usage: Simply run the main method in the MagicAnalyzer class, specify the number of pages to be read from pageList.csv, and the expected/normal DOM loading time and change the pageList.csv. Results will be written in Result.csv and Errors.csv

This was built because a client had requested performance analysis of over 20,000 pages. Other tools did not satisfy my needs; hence Performium


Expected/normal DOM time (or anamoly retryer) will re-test the page if the DOM loading time is higher. Sometimes the same page will load slowly once, but fast a hundred times; so we need to re-test the page again to get a proper reading.

Future implementations:
-Checking response code
