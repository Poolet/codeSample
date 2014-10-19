"""
Created on Nov 6, 2013

@author: TPoole
"""

#Import libraries that are needed
import sys
import argparse
import math
import functools
from decimal import Decimal

#http://www.secnetix.de/olli/Python/lambda_functions.hawk
"""
Used the above to learn about lambda functions
"""
#http://docs.python.org/3.3/library/stdtypes.html#old-string-formatting
#http://docs.python.org/3.3/tutorial/inputoutput.html
"""
Used the above to see how python handles old-string style formatting, and how the .format() function works
"""
#http://docs.python.org/dev/library/argparse.html
"""
Used the above documentation to familiarize myself with argparse. argparse seemed like the best option
to use for complex command line arguments. Additionally, it is the recommended way to parse arguments
into python(from what I read)
"""
#http://stackoverflow.com/questions/6189956/easy-way-of-finding-decimal-places
"""
Used the above while figuring out a method of determining the precision of numbers passed in in order
to determine the maximum possible precision for format
"""
#http://stackoverflow.com/questions/5920643/add-an-item-between-each-item-already-in-the-list
"""
Used the above while figuring out a method to add a separation character between each element that will be printed
"""
#http://stackoverflow.com/questions/6800481/python-map-object-is-not-subscriptable
"""
Used the above when trying to debug an error I got in my program when compiling with python3.
"""
#http://stackoverflow.com/questions/379906/parse-string-to-float-or-int
"""
Used the above when searching for a good way to choose whether to parse my input as a string or
as an int
"""

#createUnformatted is function that I use in order to create a list containing each value that needs to be output. It works for both floats and ints
def createUnformatted(start, stop, increment):
    if(increment > 0):
            while start <= stop:
                yield start
                start += increment
    else:
            while start >= stop:
                yield start
                start += increment
                
#createSeq generates a list of numbers corresponding to the arguments the user inputs. It then generates noSep which calls the format method
#on each member of seqRange. Finally it adds the separator character to each element and then returns this list.
def createSeq(first, last, increment, layout, sep):
    
    seqRange = list(createUnformatted(first, last, increment))
    
    noSep = []
    noSep = [layout % x for x in seqRange] 
    withSep = list(functools.reduce(lambda i,j: i+[sep,j], noSep[1:], noSep[:1]))
    withSep.append(sep)
    return withSep

#Convert the number passed in to either an int or a float.
def floatOrInt(number):
    try:
        return int(number)
    except ValueError:
        pass
    
    try:
        return float(number)
    except ValueError:
        print ("ERROR: Sequ only accepts floats and ints")

#getMaxLength takes in a number representing the highest precision possible, and adds it to the highest number of digits possible from first 
#or last. Since either first or last will be the largest digit possible when appended with the precision specified, we get the maximum length
#by converting to int in order to trim the decimal point and 0's from our input, then count the number of digits left, and then add the number 
#of digits in precision. Since we trimmed the decimal point, we need to add one to the size of maxLength if precision is greater than or equal to 1
#I floored the value before converting to int, so that we never get 9.9 ->10.0 or something like that when converting
def getMaxLength(precision, first, last):
    maxFirst = int(math.floor(float(first)))
    maxLast = int(math.floor(float(last)))
    maxLength = max(len(str(maxFirst)), len(str(maxLast))) + precision
    if(precision >= 1):
        maxLength = maxLength + 1
    return maxLength

def main():
    #Create my argument parser. I decided to use argparse after being unable to figure out how to get getopt working correctly. argparse was much
    #easier for me to figure out. After searching online I was able to figure out how to get positional operators as optional
    parser = argparse.ArgumentParser();
    parser.add_argument("-f","--format", help = "Use printf style floating-point format")
    parser.add_argument("-v", "--version", help = "Print version information", action = "version", version = "1.0")
    parser.add_argument("-s", "--separator", help= "Use inputted string to separate numbers", default = "\n")
    parser.add_argument("-w", "--equal-width", help = "Equalize width by padding with leading zeroes", action = "store_true", dest = "eqWidth")
    parser.add_argument("first", default = 1, nargs = "?")
    parser.add_argument("increment",  default = 1, nargs = "?")
    parser.add_argument("last")
    args = parser.parse_args()

    #Check if first, increment, and last should be converted to either an int or float. If they cannot be converted, then throw an exception
    first = floatOrInt(args.first)
    last = floatOrInt(args.last)
    increment = floatOrInt(args.increment)

    #Gives me the number of digits following the decimal  of the first, increment, and last arguments.
    numDecimalFirst = (abs(Decimal(str(first)).as_tuple().exponent))
    numDecimalIncrement = (abs(Decimal(str(increment)).as_tuple().exponent))
    numDecimalLast = (abs(Decimal(str(last)).as_tuple().exponent))

    maxPrecision = max(numDecimalFirst, numDecimalIncrement, numDecimalLast)

    maxLength = getMaxLength(maxPrecision, first,  last)

    #Configure our layout string which we will use to apply formatting to all the numbers in our sequence.
    if args.eqWidth:
        layout = "%0" + str(maxLength) + "." + str(maxPrecision) + "f"
    #Check to see if the --format command was used if the --equal-width command was not used.
    elif args.format:
        layout = args.format
    #If nothing was specified, then use the default format where we use %.PRECf as our precision length in case of any
    #float arguments or %g in case of not float
    else:
        if type(first) is float or type(last) is float or type(increment) is float:
            layout = "%." + str(maxPrecision) + "f"
        else:
            layout = "%g"

    #Now that we have our layout string created, we have everything we need to format our sequence. Call the createSeq function and put in our parameters
    finalSequence = createSeq(first, last, increment, layout, args.separator)
    #Finally, we need to print each number in the sequence we created. Set print to not use a terminating character at the end of what it prints
    for i in finalSequence:
        print(i, end = "")

if __name__ == "__main__":

    main()
    sys.exit()



