#CSC 205 Final Project
#####Juan Carlos Gallegos -- V00816131 -- April 15th, 2016

##Overview
The *Median Cut* colour quantization approach 'judiciously' constructs a palette of 256 colours from the larger palette of a supplied image. Since this allows each pixel to be represented by a maximum of 8 bits, the minimum storage needed by the image decreases notably.  
The **ColorQuantization** program accepts a PNG image as input, performs *Median Cut* quantization on it, and outputs a TXT file containing*:   

* the image dimensions (width & height)  
* the size of the palette (256 by default)   
* the *ordered* colour palette  
* the encoded pixel colours.  
The pixels are represented by integers in the range of **[0, SizeOfPalette)** that correspond to colours in the palette.

Separately, the program supports decompression: it accepts a TXT file with the above format, decodes it, and produces a corresponding PNG image.  
Additionally, the decompressor blurs areas where color banding is detected.

##Running the Program

###Compression

`java ColorQuantization image.png`  
  
**output**: `compr.txt` containing all compressed image data

###Decompression

`java ColorQuantization [-b] compr.txt`

**output**: `decompr.png`, the image with a new colour palette; `-b` flag enables blurring to treat colour banding.


##Implementation
###Compression

**Set-up**: PNG image is read & converted into a (1D) **Color** array.  

**Cut()**   

* recursively 'slices' RGB cubic space into 2 disjoint sets until the desired number of pixels is reached 
* then an average colour is computed for these pixel colours
* the space is sliced along the 'longest side' each time: the bounds for each colour are passed as parameters
* before slicing, the image is sorted by 'longest side' colour to ensure division at median (calls sortByRed(), sortByBlue(), sortByGreen())
* the desired number of pixels is a function of the image dimensions & the size of the palette, precisely:   
( Width x Height ) / SizeOfPalette  

**sortBy...()**

* uses bucket sort, since the range is known: all 3 implementations use the same helper method bucketsToArr()

**getCompressedFileContents()**  

* composes & returns a String adhering to above-mentioned format* 
* calls getEncodedPx()

**getEncodedPx()**  

* for each pixel in the image, finds closest colour in palette in terms of RGB space

###Decompression

**readCompressedFile()**  

* reads file, composing the specified palette and 2D array of pixels
* calls decompress() to retrieve image in 2D Color array format

**decompress()**

* simple mapping function: for each pixel key value, it finds its corresponding colour on the palette
* returns a 2D Color array