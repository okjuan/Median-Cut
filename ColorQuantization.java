/*
Juan Carlos Gallegos -- V00816131
April 12, 2016

Final Project for CSC 205 

ColorQuantization
- Compresses PNG image files by: 
--> computing a reduced color palette for the image
--> only specifying image dimensions, palette size, palette, and encoded pixel colors
--> outputs a binary file

- Decompresses a binary file that matches the compression format by:
--> mapping the encoded pixel to their corresponding colors in the palette
--> 

*/

import java.awt.Color;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.*;
import java.io.*;

public class ColorQuantization{
	// number of colors to be computed for palette
	private static final int NEW_DYN_RANGE = 256; 
	// used in cut() base case
	private static int PX_PER_BOX;
	// image attributes
	private static int IMG_WIDTH, IMG_HEIGHT;
	// contains all colors in new palette: computed in cut()
	private static Color [] NEW_PALETTE;
	// init with uncompressed image's original pixels
	private static Color [] ORIG_PX;
	// for testing: counting number of colors
	private static int COUNT;		
	// enable debugging
	private static boolean DEBUG = false;
	// enable blurring to treat colour banding
	private static boolean BLUR = false;
	// specifies format of compressed file
	private static final String format = "\"image_width image_height number_of_colors palette pixels_key_values\"\n(palette specifies rgb colors with 3 integers -- order implies corresponding pixel key)/";

	// ***********************************
	// COMPRESSION STUFF
	// ***********************************

	// prepares & returns compressed file content based on image/palette attributes & palette
	private static String getCompressedFileContents()
	{
		// file begins with image & palette attributes
		String file_contents = IMG_WIDTH+" "+IMG_HEIGHT+" "+NEW_DYN_RANGE+" ";
		// followed by palette
		for(Color c : NEW_PALETTE){ file_contents += c.getRed()+" "+c.getGreen()+" "+c.getBlue()+" "; }
		// followed by all pixels with their new colors encoded with ints in range [0,NEW_DYN_RANGE)
		file_contents += getEncodedPx();
		return file_contents;
	}

	// computes & returns encoded pixel colors as ints in range [0,NEW_DYN_RANGE) with the computed palette & original pixel colors
	private static String getEncodedPx()
	{
		String pxStr = "";
		// for each pixel in image, assign it the closest color in palette (in terms of Euclidean space)
		for(Color origCol : ORIG_PX){
			// index of color in palette & current distance from original color
			int closestColIndex = 0;
			int diffR = Math.abs(origCol.getRed()-NEW_PALETTE[closestColIndex].getRed());
			int diffG = Math.abs(origCol.getGreen()-NEW_PALETTE[closestColIndex].getGreen());
			int diffB = Math.abs(origCol.getBlue()-NEW_PALETTE[closestColIndex].getBlue());
			int currDist = (int) (Math.pow( (double) diffR, 2.0) + (int) Math.pow( (double) diffG, 2.0) + (int) Math.pow( (double) diffB, 2.0));;

			// iterate over colors in palette to find closest to current pixel
			for(int i = 1; i < NEW_PALETTE.length; i++){
				// compare distances by pairs to find closest
				/*compPos = (Math.pow( (double) NEW_PALETTE[i].getRed(), 2.0) + Math.pow( (double) NEW_PALETTE[i].getGreen(), 2.0) + Math.pow( (double) NEW_PALETTE[i].getBlue(), 2.0));
				int compDist = (int) Math.abs( origPos - compPos );*/
				int compR = Math.abs(origCol.getRed()-NEW_PALETTE[i].getRed());
				int compG = Math.abs(origCol.getGreen()-NEW_PALETTE[i].getGreen());
				int compB = Math.abs(origCol.getBlue()-NEW_PALETTE[i].getBlue());
				int compDist = (int) (Math.pow( (double) compR, 2.0) + (int) Math.pow( (double) compG, 2.0) + (int) Math.pow( (double) compB, 2.0));

				// if new color is closer, replace 
				if(compDist < currDist){
					currDist = compDist;
					closestColIndex = i;
				}
			}
			pxStr += closestColIndex+" ";
		}
		return pxStr;
	}

	// px : all pixels in current box. we keep 'slicing the box' until it reaches the desired size
	public static void cut(Color [] px, int startR, int endR, int startG, int endG, int startB, int endB)
	{
		// base case
		if(px.length <= PX_PER_BOX){
			try{
				NEW_PALETTE[COUNT++] = findMeanColor(px);	// store box & return
			}catch(IndexOutOfBoundsException e){
				System.out.println("More boxes computed than expected; color skipped");
			}

		// o.w. divide box into 2 disjoint boxes
		}else/* if(COUNT < NEW_DYN_RANGE)*/{
			// compute ranges of each color ("length of each side of the current cube")
			int difR = endR-startR, difG = endG-startG, difB = endB-startB;

			// find longest side & sort with respect to the color
			if(difR >= difG && difR >= difB){
				// cut along red
				px = sortByRed(px);

				// slice
				Color [] px1 = Arrays.copyOfRange(px, 0, px.length/2);
				Color [] px2 = Arrays.copyOfRange(px, px.length/2, px.length);

				// find red intensity of median
				int midR = px[px.length/2].getRed();

				cut(px1, startR, midR, startG, endG, startB, endB);
				cut(px2, midR, endR, startG, endG, startB, endB);
			
			}else if(difG >= difR && difG >= difB){
				// cut along green
				px = sortByGreen(px);

				// slice
				Color [] px1 = Arrays.copyOfRange(px, 0, px.length/2);
				Color [] px2 = Arrays.copyOfRange(px, px.length/2, px.length);

				// find green intensity of median
				int midG = px[px.length/2].getGreen();

				cut(px1, startR, endR, startG, midG, startB, endB);
				cut(px2, startR, endR, midG, endG, startB, endB);

			}else{
				// cut along blue
				px = sortByBlue(px);

				// slice
				Color [] px1 = Arrays.copyOfRange(px, 0, px.length/2);
				Color [] px2 = Arrays.copyOfRange(px, px.length/2, px.length);

				// find blue intensity of median
				int midB = px[px.length/2].getBlue();

				cut(px1, startR, endR, startG, endG, startB, midB);
				cut(px2, startR, endR, startG, endG, midB, endB);
			}
		}
	}

	// transfers colors in buckets to a Color array
	private static Color [] bucketsToArr(ArrayList<LinkedList<Color>> buckets, int len){
		//System.out.println(buckets);
		Color [] sorted = new Color[len];
		int index = 0;
		// transfer colors in buckets to an array
		for(int i = 0; i < NEW_DYN_RANGE; i++){
			LinkedList<Color> colors = buckets.get(i);
			while(!colors.isEmpty()){ sorted[index++] = colors.poll(); }
		}
		return sorted;
	}

	// runs bucket sort on px, ordering by red values
	private static Color [] sortByRed(Color [] px){
		// buckets numbered 0-->255
		ArrayList<LinkedList<Color>> buckets = new ArrayList<LinkedList<Color>>(NEW_DYN_RANGE);
		// go through array list and create lists
		for(int i = 0; i < NEW_DYN_RANGE; i++){ buckets.add(new LinkedList<Color>()); }

		// place in buckets
		for(Color c : px){ 
			// bucket number corresponds to red channel intensity of color
			LinkedList<Color> list = buckets.get(c.getRed());
			// place in bucket
			list.add(c); 
		}
		Color [] result = bucketsToArr(buckets, px.length);
		// ensure sorted order
		//for(int i = 0; i < result.length-1; i++){ assert result[i].getRed() <= result[i+1].getRed(); }
		return result;
	}

	// runs bucket sort on px, ordering by green values
	private static Color [] sortByGreen(Color [] px){
		// buckets numbered 0-->255
		ArrayList<LinkedList<Color>> buckets = new ArrayList<LinkedList<Color>>(NEW_DYN_RANGE);
		// go through array list and create lists
		for(int i = 0; i < NEW_DYN_RANGE; i++){ buckets.add(new LinkedList<Color>()); }
		
		// place in buckets
		for(Color c : px){ 
			// bucket number corresponds to red channel intensity of color
			LinkedList<Color> list = buckets.get(c.getGreen());
			// place in bucket
			list.add(c); 
		}
		Color [] result = bucketsToArr(buckets, px.length);
		// ensure sorted order
		//for(int i = 0; i < result.length-1; i++){ assert result[i].getGreen() <= result[i+1].getGreen(); }
		return result;
	}

	// runs bucket sort on px, ordering by blue values
	private static Color [] sortByBlue(Color [] px){
		// buckets numbered 0-->255
		ArrayList<LinkedList<Color>> buckets = new ArrayList<LinkedList<Color>>();
		// go through array list and create lists
		for(int i = 0; i < NEW_DYN_RANGE; i++){ buckets.add(new LinkedList<Color>()); }

		// place in buckets
		for(Color c : px){
			// bucket number corresponds to red channel intensity of color
			LinkedList<Color> list = buckets.get(c.getBlue());
			// place in bucket
			list.add(c); 
		}
		Color [] result = bucketsToArr(buckets, px.length);
		// ensure sorted order
		//for(int i = 0; i < result.length-1; i++){ assert result[i].getBlue() <= result[i+1].getBlue(); }
		return result;
	}

	// find average color of given array
	private static Color findMeanColor(Color [] px)
	{
		int sumR = 0, sumG = 0, sumB = 0;
		// sum intensities per color channel
		for(Color c : px){ sumR += c.getRed(); sumG += c.getGreen(); sumB += c.getBlue(); }
		// find ave intensity per color channel
		int meanR = (int) (((double)sumR/(double)px.length) + 0.5), meanG = (int) (((double)sumG/(double)px.length) + 0.5), meanB = (int) (((double)sumB/(double)px.length) + 0.5);
		// clamp
		meanR = (meanR > 255? 255 : meanR); meanG = (meanG > 255? 255 : meanG); meanB = (meanB > 255? 255 : meanB);
		return(new Color(meanR, meanG, meanB));
	}

	// ***********************************
	// MAIN & TESTING STUFF
	// ***********************************
	public static void main(String [] args)
	{
		int index = 0;
		String flag = "";

		// check for command line argument
		if(args.length == 0){ 
			System.out.println("Usage: java ColorQuantization <input_filename> <output_filename>");
			System.exit(-1);
		}else if(args.length == 2){
			flag = args[index++];
			DEBUG = (flag.equals("-d")? true : false);
			BLUR = (flag.equals("-b")? true : false);
		}
		
		String in_file = args[index]; 

		// DECOMPRESSION
		if(in_file.endsWith(".txt")){
			/*if(BIN){
				Color [][] img = readCompressedBinaryFile(in_file);
			}else{
				Color [][] img = readCompressedFile(in_file);
			}*/
			Color [][] img = readCompressedFile(in_file);
			savePNG(img, "decompr.png");

		// COMPRESSION
		}else if(in_file.endsWith(".png")){
			// convert pixels in image to Color array
			Color [] px = getPx(in_file);
			// then compute desired 'box' size according to # of pixels in image & desired dynamic range
			PX_PER_BOX = (int) Math.ceil((double)IMG_WIDTH * (double)IMG_HEIGHT / (double)NEW_DYN_RANGE);
			// prepare to compute palette
			COUNT = 0;
			NEW_PALETTE = new Color[NEW_DYN_RANGE];
			// compute palette using median cut
			System.out.println("Computing palette...");
			cut(px, 0, NEW_DYN_RANGE, 0, NEW_DYN_RANGE, 0, NEW_DYN_RANGE);

			if(DEBUG){ printPalette(); }

			System.out.println("Preparing output file contents...");
			String txt_file_contents = getCompressedFileContents();

			try{
				PrintStream out = new PrintStream(new File("compr.txt"));
				out.print(txt_file_contents);
				System.out.println("Compression complete: wrote to compr.txt");
			}catch(FileNotFoundException e){
				System.out.println("Unable to create file: compr.txt");
				System.out.println(e);
				System.exit(-1);
			}
	
			/*if(BIN){
				byte [] bin_file_contents = txt_file_contents.getBytes();
				try{
					FileOutputStream out = new FileOutputStream(new File("compr.bin"));
					out.write(bin_file_contents, 0, bin_file_contents.length);
					out.flush();
					out.close();
					System.out.println("Compression complete: wrote to compr.bin");
				}catch(IOException e){
					System.out.println("Unable to create file: compr.bin");
					System.out.println(e);
					System.exit(-1);
				}
			}else{
				try{
					PrintStream out = new PrintStream(new File("compr.txt"));
					out.print(txt_file_contents);
					System.out.println("Compression complete: wrote to compr.txt");
				}catch(FileNotFoundException e){
					System.out.println("Unable to create file: compr.txt");
					System.out.println(e);
					System.exit(-1);
				}
			}*/

		}else{
			System.out.println("File must have suffix '.txt' (compressed image file) or '.png' (image file)");
			System.exit(-1);
		}

	}

	// prints palette to see colors
	private static void printPalette(){
		Color [][] squarePalette = new Color[16][16];
		int ctr = 0;
		for(int i = 0; i < squarePalette.length; i++){
			for(int j = 0; j < squarePalette[0].length; j++){
				try{squarePalette[i][j] = NEW_PALETTE[ctr++];}catch(IndexOutOfBoundsException e){System.out.println(e); System.exit(-1);}
			}
		}
		savePNG(squarePalette, "palette.png");
	}

	private static void dump2DArr(int [][] arr){
		for(int [] row : arr){
			for(int num : row){
				System.out.print(num+" ");
			}
			System.out.println();
		}
	}

	// print colors in array
	private static void dumpCol(Color [] arr){
		for(Color c : arr){
			System.out.print("r: "+c.getRed()+" g: "+c.getGreen()+" b: "+c.getBlue()+", ");
		}
	}

	// ***********************************
	// DECOMPRESSOR STUFF
	// ***********************************
	// decodes Color value of each pixel from supplied key values and corresponding palette
	private static Color [][] decompressAndBlur(Color [] palette, int [][] px)
	{
		Color [][] img = new Color[px.length][px[0].length];

		int [][] sumDiff = new int[px.length][px[0].length];
		int minNotZero = 255*3, totalSumDiff = 0, numDiff = 0;
		boolean checkUp = false;
		boolean checkLeft, checkRight;
		// for each pixel: find & store color associated with key value provided
		// can check above pixel from the second row til the end
		for(int i = 0; i < px.length; i++, checkUp = true){
			checkLeft = false;	// can check left pixel from second pixel on row til end
			checkRight = true;	// can check right pixel from first pixel on row til 2nd to last
			for(int j = 0; j < px[0].length; j++, checkLeft = true){
				img[i][j] = palette[px[i][j]];	// palette[key value] = Color instance

				// cannot check upRight pixel if at rightmost pixel on row
				if(j == px[0].length-1){ checkRight = false; }

				int currRed = img[i][j].getRed(), currGreen = img[i][j].getGreen(), currBlue = img[i][j].getBlue();
				int diff;
				if(checkUp){
					int upRed = img[i-1][j].getRed(), upGreen = img[i-1][j].getGreen(), upBlue = img[i-1][j].getBlue();
					diff = Math.abs(currRed - upRed) + Math.abs(currGreen - upGreen) + Math.abs(currRed - upBlue);
					sumDiff[i][j] += diff;
					sumDiff[i-1][j] += diff;

					numDiff++;
					totalSumDiff += diff;
					if(diff < minNotZero && diff != 0){ minNotZero = diff; }
				}
				if(checkLeft){
					int leftRed = img[i][j-1].getRed(), leftGreen = img[i][j-1].getGreen(), leftBlue = img[i][j-1].getBlue();
					diff = Math.abs(currRed - leftRed) + Math.abs(currGreen - leftGreen) + Math.abs(currRed - leftBlue);
					sumDiff[i][j] += diff;
					sumDiff[i][j-1] += diff;

					numDiff++;
					totalSumDiff += diff;
					if(diff < minNotZero && diff != 0){ minNotZero = diff; }
				}
				if(checkLeft && checkUp){
					int upLeftRed = img[i-1][j-1].getRed(), upLeftGreen = img[i-1][j-1].getGreen(), upLeftBlue = img[i-1][j-1].getBlue();
					diff = Math.abs(currRed - upLeftRed) + Math.abs(currGreen - upLeftGreen) + Math.abs(currRed - upLeftBlue);
					sumDiff[i][j] += diff;
					sumDiff[i-1][j-1] += diff;

					numDiff++;
					totalSumDiff += diff;
					if(diff < minNotZero && diff != 0){ minNotZero = diff; }
				}
				if(checkRight && checkUp){
					int upRightRed = img[i-1][j+1].getRed(), upRightGreen = img[i-1][j+1].getGreen(), upRightBlue = img[i-1][j+1].getBlue();
					diff = Math.abs(currRed - upRightRed) + Math.abs(currGreen - upRightGreen) + Math.abs(currRed - upRightBlue);
					sumDiff[i][j] += diff;
					sumDiff[i-1][j+1] += diff;

					numDiff++;
					totalSumDiff += diff;
					if(diff < minNotZero && diff != 0){ minNotZero = diff; }
				}
			}
		}
		int aveDiff = totalSumDiff/(numDiff);
		return blur(minNotZero, aveDiff, img, sumDiff);
	}

	private static Color [][] blur(int minDiffNotZero, int aveDiff, Color [][] img, int [][] sumDiff)
	{
		double factor = 1.0/5.0;
		int radius = 3;
		//***********
		// COMMENT
		//**********
		// to specify range of difference where filter should be applied
		return applyBlur(minDiffNotZero, aveDiff/5, sumDiff, img, factor, 1);
	}

	// applies a NxN filter to a grayscale image, where N ~ odd
	private static Color [][] applyBlur(int lb, int ub, int [][] sumDiff, Color [][] img, double factor, int radius)
	{
		Color [][] newImage = new Color [ img.length ][ img[0].length ];
		// for each pixel in the image
		for(int i = 0; i < img.length; i++){
			for(int j = 0; j < img[0].length; j++){

				// if pixel colour differs from neighboring colours by small range, blur (colour banding)
				if(lb <= sumDiff[i][j] && sumDiff[i][j] <= ub){
					double newRed = 0, newGreen = 0, newBlue = 0;
					double lastRed = img[i][j].getRed(), lastGreen = img[i][j].getGreen(), lastBlue = img[i][j].getBlue();
					// apply filter to pixel: nested loops iterate through filter & image region
					for(int col = i-radius; col <= i+radius; col++){
						for(int row = j-radius; row <= j+radius; row++){
							// if not in range, set equal to color at i,j (current px colour) 
							int diff = 0;
							if(col < 0 || img.length-1 < col || row < 0 || img[0].length-1 < row){
								newRed += (factor * (double) img[i][j].getRed());
								newGreen += (factor * (double) img[i][j].getGreen());
								newBlue += (factor * (double) img[i][j].getBlue());
							}else{	
								diff += Math.abs(img[col][row].getRed() - img[i][j].getRed()) ;
								diff += Math.abs(img[col][row].getGreen() - img[i][j].getGreen()) ;
								diff += Math.abs(img[col][row].getBlue() - img[i][j].getBlue()) ;

								if(lb < diff && diff < ub){
									newRed += (factor * (double) img[col][row].getRed());
									newGreen += (factor * (double) img[col][row].getGreen());
									newBlue += (factor * (double) img[col][row].getBlue());
								}else{
									newRed = lastRed;
									newGreen = lastGreen;
									newBlue = lastBlue;
								}
							}
							lastRed = newRed;
							lastGreen = newGreen;
							lastBlue = newBlue;
						}
					}
					// clamp
					if(255.0 < newRed){ newRed = 255.0; }
					if(255.0 < newGreen){ newGreen = 255.0; }
					if(255.0 < newBlue){ newBlue = 255.0; }
					// save pixel
					newImage[i][j] = new Color((int) newRed, (int) newGreen, (int) newBlue);
				}else{
					newImage[i][j] = img[i][j];
				}
			}
		}
		return newImage;
	}


	private static Color [][] decompress(Color [] palette, int [][] px)
	{
		if(BLUR){ return decompressAndBlur(palette, px); }

		Color [][] img = new Color[px.length][px[0].length];
		// for each pixel: find & store color associated with key value provided
		// can check above pixel from the second row til the end
		for(int i = 0; i < px.length; i++){
			for(int j = 0; j < px[0].length; j++){
				img[i][j] = palette[px[i][j]];	// palette[key value] = Color instance
			}
		}
		return img;
	}

	// reads formatted txt file to acquire image data; returns 2D Color array rep of image
	public static Color [][] readCompressedFile(String filename)
	{
		Color [][] image = null;
		Scanner in;
		try{
			in = new Scanner(new File(filename));
			// read in image & palette attributes
			int imgWidth = in.nextInt();
			int imgHeight = in.nextInt();
			int numColors = in.nextInt();

			Color [] palette = new Color[numColors];
			// read in all colors in palette
			for(int i = 0; i < numColors; i++){ palette[i] = new Color(in.nextInt(), in.nextInt(), in.nextInt()); }

			int [][] px = new int [imgWidth][imgHeight];
			// collects key values for all pixels
			for(int i = 0; i < imgWidth; i++){
				for(int j = 0; j < imgHeight; j++){
					px[i][j] = in.nextInt();
				}
			}
			// decompresses info into 2D Color array (the image!)
			image = decompress(palette, px);
		}catch(InputMismatchException e){
			System.out.println("File contents do not adhere to expected format: \n"+format);
			System.out.println(e);
			System.exit(-1);
		}catch(FileNotFoundException e){
			System.out.println("Could not read file: "+filename);
			System.out.println(e);
			System.exit(-1);
		}
		return image;
	}


	// ***********************************
	// IMAGE READING & WRITING STUFF
	// ***********************************
	private static Color [] getPx(String image_filename)
	{
		BufferedImage inputImage = null;
		try{
			System.err.printf("Reading image from %s\n",image_filename);
			inputImage = ImageIO.read(new File(image_filename));
		} catch(java.io.IOException e){
			System.out.println("Unable to open: "+image_filename);
			System.out.println(e);
			System.exit(-1);
		}

		IMG_WIDTH = inputImage.getWidth();
		IMG_HEIGHT = inputImage.getHeight();

		Color [] imgPx = new Color[IMG_WIDTH*IMG_HEIGHT];
		ORIG_PX = new Color[IMG_WIDTH*IMG_HEIGHT];

		int ctr = 0;
		for (int i = 0; i < IMG_WIDTH; i++){
			for(int j = 0; j < IMG_HEIGHT; j++){
				// append color to array
				imgPx[ctr] = new Color(inputImage.getRGB(i,j));
				ORIG_PX[ctr++] = new Color(inputImage.getRGB(i,j));
			}
		}

		System.err.printf("Read a %d by %d image\n",IMG_WIDTH,IMG_HEIGHT);
		return imgPx;
	}

	private static void savePNG(Color[][] imagePixels, String image_filename)
	{
		int width = imagePixels.length;
		int height = imagePixels[0].length;
		BufferedImage outputImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < width; x++)
			for(int y = 0; y < height; y++)
				outputImage.setRGB(x,y,imagePixels[x][y].getRGB());
		
		try{
			ImageIO.write(outputImage, "png", new File(image_filename));
		}catch(java.io.IOException e){
			System.out.println("Unable to write to file: "+image_filename);
			System.out.println(e);
			System.exit(-1);
		}
		System.err.printf("Wrote a %d by %d image\n",width,height);
	}
}