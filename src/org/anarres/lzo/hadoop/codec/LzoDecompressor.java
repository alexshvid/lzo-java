/*
 * This file is part of Hadoop-Gpl-Compression.
 *
 * Hadoop-Gpl-Compression is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Hadoop-Gpl-Compression is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hadoop-Gpl-Compression.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.anarres.lzo.hadoop.codec;

import java.io.IOException;
import org.anarres.lzo.LzoConstants;
import org.anarres.lzo.LzoDecompressor1x;
import org.anarres.lzo.LzoErrors;
import org.anarres.lzo.lzo_uintp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.compress.Decompressor;

/**
 * A {@link Decompressor} based on the lzo algorithm.
 * http://www.oberhumer.com/opensource/lzo/
 * 
 */
public class LzoDecompressor implements Decompressor {

	private static final Log LOG = LogFactory.getLog(LzoDecompressor.class);

	private final CompressionStrategy strategy;
	private final byte[] outputBuffer;
	private int outputBufferPos;
	private final lzo_uintp outputBufferLen = new lzo_uintp();	// Also, end, since we base outputBuffer at 0.


	public static enum CompressionStrategy {

		/**
		 * lzo1 algorithms.
		 */
		LZO1(0),
		/**
		 * lzo1a algorithms.
		 */
		LZO1A(1),
		/**
		 * lzo1b algorithms.
		 */
		LZO1B(2),
		LZO1B_SAFE(3),
		/**
		 * lzo1c algorithms.
		 */
		LZO1C(4),
		LZO1C_SAFE(5),
		LZO1C_ASM(6),
		LZO1C_ASM_SAFE(7),
		/**
		 * lzo1f algorithms.
		 */
		LZO1F(8),
		LZO1F_SAFE(9),
		LZO1F_ASM_FAST(10),
		LZO1F_ASM_FAST_SAFE(11),
		/**
		 * lzo1x algorithms.
		 */
		LZO1X(12),
		LZO1X_SAFE(13),
		LZO1X_ASM(14),
		LZO1X_ASM_SAFE(15),
		LZO1X_ASM_FAST(16),
		LZO1X_ASM_FAST_SAFE(17),
		/**
		 * lzo1y algorithms.
		 */
		LZO1Y(18),
		LZO1Y_SAFE(19),
		LZO1Y_ASM(20),
		LZO1Y_ASM_SAFE(21),
		LZO1Y_ASM_FAST(22),
		LZO1Y_ASM_FAST_SAFE(23),
		/**
		 * lzo1z algorithms.
		 */
		LZO1Z(24),
		LZO1Z_SAFE(25),
		/**
		 * lzo2a algorithms.
		 */
		LZO2A(26),
		LZO2A_SAFE(27);
		private final int decompressor;

		private CompressionStrategy(int decompressor) {
			this.decompressor = decompressor;
		}

		int getDecompressor() {
			return decompressor;
		}
	}; // CompressionStrategy

	/**
	 * Creates a new lzo decompressor.
	 *
	 * @param strategy lzo decompression algorithm
	 * @param directBufferSize size of the direct-buffer
	 */
	public LzoDecompressor(CompressionStrategy strategy, int outputBufferSize) {
		this.strategy = strategy;
		this.outputBuffer = new byte[outputBufferSize];
	}

	/**
	 * Creates a new lzo decompressor.
	 */
	public LzoDecompressor() {
		this(CompressionStrategy.LZO1X, 64 * 1024);
	}

	@Override
	public void setInput(byte[] b, int off, int len) {
		if (b == null)
			throw new NullPointerException();
		if (off < 0 || len < 0 || off > b.length - len)
			throw new ArrayIndexOutOfBoundsException("Illegal range in buffer: Buffer length=" + b.length + ", offset=" + off + ", length=" + len);
		if (!needsInput())
			throw new IllegalStateException("I don't need input: pos=" + outputBufferPos + "; len=" + outputBufferLen);
		outputBufferLen.value = outputBuffer.length;
		int code = LzoDecompressor1x.lzo1x_decompress(b, off, len, outputBuffer, 0, outputBufferLen, null);
		if (code != LzoConstants.LZO_E_OK)
			throw new IllegalArgumentException(LzoErrors.toString(code));
		outputBufferPos = 0;
	}

	@Override
	public void setDictionary(byte[] b, int off, int len) {
		// nop
	}

	@Override
	public boolean needsInput() {
		return outputBufferLen.value <= 0;
	}

	@Override
	public boolean needsDictionary() {
		return false;
	}

	@Override
	public boolean finished() {
		return outputBufferLen.value <= 0;
	}

	@Override
	public int decompress(byte[] b, int off, int len)
			throws IOException {
		if (b == null)
			throw new NullPointerException();
		if (off < 0 || len < 0 || off > b.length - len)
			throw new ArrayIndexOutOfBoundsException("Illegal range in buffer: Buffer length=" + b.length + ", offset=" + off + ", length=" + len);

		len = Math.min(len, outputBufferLen.value);
		System.arraycopy(outputBuffer, outputBufferPos, b, off, len);
		outputBufferPos += len;
		outputBufferLen.value -= len;

		return len;
	}

	@Override
	public void reset() {
		outputBufferPos = 0;
		outputBufferLen.value = 0;
	}

	@Override
	public void end() {
	}
}