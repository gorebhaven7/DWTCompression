import static java.lang.Math.cos;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class DWTCompression {
    private static int imgWith = 512;
    private static int imgHeight = 512;

    JFrame jFrame = new JFrame();
    JLabel jLabel_img = new JLabel();

    BufferedImage OrgImage = new BufferedImage(imgWith, imgHeight, BufferedImage.TYPE_INT_RGB);
    BufferedImage DwtImage = new BufferedImage(imgWith, imgHeight, BufferedImage.TYPE_INT_RGB);;

    static double[][] Matrixblock = new double[8][8];
    int[][] R_Matrix = new int[imgHeight][imgWith];
    int[][] G_Matrix = new int[imgHeight][imgWith];
    int[][] B_Matrix = new int[imgHeight][imgWith];

    double[][] DWT_R_Matrix = new double[imgHeight][imgWith];
    double[][] DWT_G_Matrix = new double[imgHeight][imgWith];
    double[][] DWT_B_Matrix = new double[imgHeight][imgWith];

    int[][] IDWT_R_Matrix = new int[imgHeight][imgWith];
    int[][] IDWT_G_Matrix = new int[imgHeight][imgWith];
    int[][] IDWT_B_Matrix = new int[imgHeight][imgWith];

    private static final int[] arr_n = {1, 3, 10, 40, 140, 500, 2000, 16384, 65536, 262144};

    public static void main(String[] args) {

        DWTCompression ren = new DWTCompression();
        ren.showImage(args);
    }

    public void showImage(String[] args) {

        try {
            InputStream is = new FileInputStream(new File(args[0]));
            byte[] bytes = new byte[(int) new File(args[0]).length()];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                    && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            Calculateoffset(bytes);
            CalculateInitialCosineTransform(args);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void Calculateoffset(byte[] size) {
        int ind = 0;
        int y = 0;
        while (y < imgHeight) {
            int x = 0;
            while (x < imgWith) {
                int r = size[ind];
                int g = size[ind + imgHeight * imgWith];
                int b = size[ind + imgHeight * imgWith * 2];

                r = r & 0xFF;
                g = g & 0xFF;
                b = b & 0xFF;

                R_Matrix[y][x] = r;
                G_Matrix[y][x] = g;
                B_Matrix[y][x] = b;

                int pixOriginal = 0xff000000 | ((r & 0xff) << 16)
                        | ((g & 0xff) << 8) | (b & 0xff);
                OrgImage.setRGB(x, y, pixOriginal);
                ind++;
                x++;
            }
            y++;
        }
    }

    private void CalculateInitialCosineTransform(String[] args) {

        int p = 0;
        while (p < 8) {
            int q = 0;
            while (q < 8) {
                Matrixblock[p][q] = cos((2 * p + 1) * q * 3.14159 / 16.00);
                q++;
            }
            p++;
        }
        int n = Integer.parseInt(args[1]);
        if (n != -1) {
            n = arr_n[Integer.parseInt(args[1])];
            DWT_R_Matrix = dwtStandardDecomposition(R_Matrix, n);
            DWT_G_Matrix = dwtStandardDecomposition(G_Matrix, n);
            DWT_B_Matrix = dwtStandardDecomposition(B_Matrix, n);

            IDWT_R_Matrix = idwtComposition(DWT_R_Matrix);
            IDWT_G_Matrix = idwtComposition(DWT_G_Matrix);
            IDWT_B_Matrix = idwtComposition(DWT_B_Matrix);

            displayDwtImage(0);
            displayImg(0);
        } else {
            for (int i = 0; i < arr_n.length; i++) {
                int nValue = arr_n[i];
                DWT_R_Matrix = dwtStandardDecomposition(R_Matrix, nValue);
                DWT_G_Matrix = dwtStandardDecomposition(G_Matrix, nValue);
                DWT_B_Matrix = dwtStandardDecomposition(B_Matrix, nValue);

                IDWT_R_Matrix = idwtComposition(DWT_R_Matrix);
                IDWT_G_Matrix = idwtComposition(DWT_G_Matrix);
                IDWT_B_Matrix = idwtComposition(DWT_B_Matrix);

                displayDwtImage(i);
                displayImg(i);

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void displayDwtImage(int iteration) {
        int y = 0;
        while (y < imgHeight) {
            int x = 0;
            while (x < imgWith) {
                int rr = (int) IDWT_R_Matrix[y][x];
                int gg = (int) IDWT_G_Matrix[y][x];
                int bb = (int) IDWT_B_Matrix[y][x];

                int pixx = 0xff000000 | ((rr & 0xff) << 16) | ((gg & 0xff) << 8) | (bb & 0xff);
                DwtImage.setRGB(x, y, pixx);
                x++;
            }
            y++;
        }

        jLabel_img.setIcon(new ImageIcon(DwtImage));
        jFrame.pack();
    }

    public void displayImg(int iteration) {
        if (iteration == 0) {
            jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            jFrame.setLayout(new GridBagLayout());
            jLabel_img.setHorizontalAlignment(SwingConstants.CENTER);
            jLabel_img.setIcon(new ImageIcon(DwtImage));
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 0.5;
            c.gridx = 1;
            c.gridy = 0;
            jFrame.getContentPane().add(jLabel_img, c);
            jFrame.pack();
            jFrame.setVisible(true);
        }
    }
private double[][] transpose(double[][] matrix) {
		double[][] result = new double[imgHeight][imgWith];
		int y = 0;
		while (y < imgHeight) {
			int x = 0;
			while (x < imgWith) {
				result[y][x] = matrix[x][y];
				x++;
			}
			y++;
		}
		return result;
	}

	private double[][] dwtStandardDecomposition(int[][] matrix, int n) {
		double[][] dMatrix = new double[imgHeight][imgWith];
		int y = 0;
		while (y < imgHeight) {
			int x = 0;
			while (x < imgWith) {
				dMatrix[y][x] = matrix[y][x];
				x++;
			}
			y++;

		}

		int r = 0;
		while (r < imgWith) {
			dMatrix[r] = decomposition(dMatrix[r]);
			r++;
		}
		int c = 0;
		dMatrix = transpose(dMatrix);
		while (c < imgHeight) {
			dMatrix[c] = decomposition(dMatrix[c]);
			c++;
		}
		dMatrix = transpose(dMatrix);
		dMatrix = ZagTraver(dMatrix, n);
		return dMatrix;
	}

	private double[] decomposition(double[] array) {
		int h = array.length;
		while (h > 0) {
			array = decompositionStep(array, h);
			h = h / 2;
		}
		return array;
	}

	private double[] decompositionStep(double[] array, int h) {
		double[] dArray = Arrays.copyOf(array, array.length);
		int k = 0;
		while (k < h / 2) {
			dArray[k] = (array[2 * k] + array[2 * k + 1]) / 2; 
			dArray[h / 2 + k] = (array[2 * k] - array[2 * k + 1]) / 2; 
			k++;
		}
		return dArray;
	}

	private int[][] idwtComposition(double[][] matrix) {
		int[][] iMatrix = new int[imgHeight][imgWith];

		matrix = transpose(matrix);
		for (int col = 0; col < imgHeight; col++) {
			matrix[col] = composition(matrix[col]);
		}
		matrix = transpose(matrix);
		for (int row = 0; row < imgWith; row++) {
			matrix[row] = composition(matrix[row]);
		}

		int y = 0;
		while (y < imgHeight) {
			int x = 0;
			while (x < imgWith) {
				iMatrix[y][x] = (int) Math.round(matrix[y][x]);
				if (iMatrix[y][x] < 0) {
					iMatrix[y][x] = 0;
				}
				if (iMatrix[y][x] > 255) {
					iMatrix[y][x] = 255;
				}
				x++;
			}
			y++;
		}
		return iMatrix;
	}

	private double[] composition(double[] array) {
		int h = 1;
		while (h <= array.length) {
			array = compositionStep(array, h);
			h = h * 2;
		}
		return array;
	}

	private double[] compositionStep(double[] array, int h) {
		double[] dArray = Arrays.copyOf(array, array.length);
		for (int i = 0; i < h / 2; i++) {
			dArray[2 * i] = array[i] + array[h / 2 + i];
			dArray[2 * i + 1] = array[i] - array[h / 2 + i];
		}
		return dArray;
	}


	public double[][] ZagTraver(double[][] matrix, int m) {
		int i = 0;
		int j = 0;
		int length = matrix.length - 1;
		int count = 1;

		if (count > m) {
			matrix[i][j] = 0;
			count++;
		} else {
			count++;
		}

		while (true) {

			j++;
			if (count > m) {
				matrix[i][j] = 0;
				count++;
			} else {
				count++;
			}

			while (j != 0) {
				i++;
				j--;

				if (count > m) {
					matrix[i][j] = 0;
					count++;
				} else {
					count++;
				}
			}
			i++;
			if (i > length) {
				i--;
				break;
			}

			if (count > m) {
				matrix[i][j] = 0;
				count++;
			} else {
				count++;
			}

			while (i != 0) {
				i--;
				j++;
				if (count > m) {
					matrix[i][j] = 0;
					count++;
				} else {
					count++;
				}
			}
		}

		while (true) {
			j++;
			if (count > m) {
				matrix[i][j] = 0;
				count++;
			} else {
				count++;
			}

			while (j != length) {
				j++;
				i--;

				if (count > m) {
					matrix[i][j] = 0;
					count++;
				} else {
					count++;
				}
			}
			i++;
			if (i > length) {
				i--;
				break;
			}

			if (count > m) {
				matrix[i][j] = 0;
				count++;
			} else {
				count++;
			}

			while (i != length) {
				i++;
				j--;
				if (count > m) {
					matrix[i][j] = 0;
					count++;
				} else {
					count++;
				}
			}
		}
		return matrix;
	}
}