/*
 * This file is part of Fim - File Integrity Manager
 *
 * Copyright (C) 2015  Etienne Vrignaud
 *
 * Fim is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Fim is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Fim.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fim.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.fim.model.FileHash;
import org.fim.model.HashMode;
import org.fim.tooling.BuildableParameters;
import org.fim.tooling.StateAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class FileHasherTest extends StateAssert
{
	public static final String NO_HASH = "no_hash";
	private final Charset utf8 = Charset.forName("UTF-8");

	private StateGenerator stateGenerator;

	private HashMode hashMode;
	private BuildableParameters parameters;
	private String rootDir;
	private FileHasher cut;

	public FileHasherTest(final HashMode hashMode)
	{
		this.hashMode = hashMode;
	}

	@Parameterized.Parameters(name = "Hash mode: {0}")
	public static Collection<Object[]> parameters()
	{
		return Arrays.asList(new Object[][]{
				{HashMode.DONT_HASH_FILES},
				{HashMode.HASH_ONLY_FIRST_FOUR_KILO},
				{HashMode.HASH_ONLY_FIRST_MEGA},
				{HashMode.COMPUTE_ALL_HASH}
		});
	}

	@Before
	public void setup() throws NoSuchAlgorithmException
	{
		stateGenerator = mock(StateGenerator.class);
		parameters = defaultParameters();
		parameters.setHashMode(hashMode);
		when(stateGenerator.getParameters()).thenReturn(parameters);

		rootDir = "target/" + this.getClass().getSimpleName();
		cut = new FileHasher(stateGenerator, null, rootDir);
	}

	@Test
	public void weCanConvertToHexa()
	{
		byte[] bytes = new byte[]{(byte) 0xa4, (byte) 0xb0, (byte) 0xe5, (byte) 0xfd};
		String hexString = cut.toHexString(bytes);
		assertThat(hexString).isEqualTo("a4b0e5fd");
	}

	@Test
	public void weCanConvertToHexaWithZero()
	{
		byte[] bytes = new byte[]{(byte) 0xa0, 0x40, 0x0b, 0x00, (byte) 0xe0, 0x05, 0x0f, 0x0d};
		String hexString = cut.toHexString(bytes);
		assertThat(hexString).isEqualTo("a0400b00e0050f0d");
	}

	@Test
	public void weCanGetTheRelativeFileName()
	{
		String relativeFileName = cut.getRelativeFileName("/dir1/dir2/dir3", "/dir1/dir2/dir3/dir4/file1");
		assertThat(relativeFileName).isEqualTo("dir4/file1");

		relativeFileName = cut.getRelativeFileName("/dir5/dir6/dir7", "/dir1/dir2/dir3/dir4/file1");
		assertThat(relativeFileName).isEqualTo("dir1/dir2/dir3/dir4/file1");

		relativeFileName = cut.getRelativeFileName("/dir1/dir2/dir3", "dir4/file1");
		assertThat(relativeFileName).isEqualTo("dir4/file1");
	}

	@Test
	public void weCanHashALittleFile() throws IOException
	{
		File fileToHash = new File("LICENSE");
		String firstFourKiloHash = "757af34fe2d75e895caf4e479e77e5b2ba97510140933c89facc0399eb92063e83d7833d5d3285d35ee310b6d599aa8f8cafbd480cb797bbb2d8b8b47880d2ba";
		String firstMegaHash = "57547468f95220e8e0e265f0682b1dc787e123fa984d12482b38ef69b6f3a8e0843f36bccf4262f3c686e6a9fb55552ed386e295f72e6401f66480d2da6145d1";
		String fullFileHash = "57547468f95220e8e0e265f0682b1dc787e123fa984d12482b38ef69b6f3a8e0843f36bccf4262f3c686e6a9fb55552ed386e295f72e6401f66480d2da6145d1";

		FileHash fileHash = cut.hashFile(fileToHash);

		assertFileHash(fileHash, firstFourKiloHash, firstMegaHash, fullFileHash);
	}

	@Test
	public void weCanHashABigFile() throws IOException
	{
		String firstFourKiloHash = "757af34fe2d75e895caf4e479e77e5b2ba97510140933c89facc0399eb92063e83d7833d5d3285d35ee310b6d599aa8f8cafbd480cb797bbb2d8b8b47880d2ba";
		String firstMegaHash = "733e3c1c2e1a71086637cecfe168a47d35c10cda2b792ff645befef7eaf86b96ecaf357b775dd323d5ab2a638c90c81abcae89372500dd8da60160508486bf4d";
		String fullFileHash = "e891a71e312bc6e34f549664706951516c42f660face62756bb155301c5e06ba79db94f83dedd43467530021935f5b427a58d7a5bd245ea1b2b0db8d7b08ee7a";

		File fileToHash = createBigLicenseFile(60 * 1024 * 1024);

		FileHash fileHash = cut.hashFile(fileToHash);

		assertFileHash(fileHash, firstFourKiloHash, firstMegaHash, fullFileHash);

		// Remove the big license file to have a little workspace
		fileToHash.delete();
	}

	private void assertFileHash(FileHash fileHash, String firstFourKiloHash, String firstMegaHash, String fullFileHash)
	{
		switch (hashMode)
		{
			case DONT_HASH_FILES:
				assertThat(fileHash.getFirstFourKiloHash()).isEqualTo(NO_HASH);
				assertThat(fileHash.getFirstMegaHash()).isEqualTo(NO_HASH);
				assertThat(fileHash.getFullHash()).isEqualTo(NO_HASH);
				break;

			case HASH_ONLY_FIRST_FOUR_KILO:
				assertThat(fileHash.getFirstFourKiloHash()).isEqualTo(firstFourKiloHash);
				assertThat(fileHash.getFirstMegaHash()).isEqualTo(NO_HASH);
				assertThat(fileHash.getFullHash()).isEqualTo(NO_HASH);
				break;

			case HASH_ONLY_FIRST_MEGA:
				assertThat(fileHash.getFirstFourKiloHash()).isEqualTo(NO_HASH);
				assertThat(fileHash.getFirstMegaHash()).isEqualTo(firstMegaHash);
				assertThat(fileHash.getFullHash()).isEqualTo(NO_HASH);
				break;

			case COMPUTE_ALL_HASH:
				assertThat(fileHash.getFirstFourKiloHash()).isEqualTo(firstFourKiloHash);
				assertThat(fileHash.getFirstMegaHash()).isEqualTo(firstMegaHash);
				assertThat(fileHash.getFullHash()).isEqualTo(fullFileHash);
				break;
		}
	}

	private File createBigLicenseFile(long fileSize) throws IOException
	{
		File license = new File("LICENSE");
		String content = FileUtils.readFileToString(license, utf8);

		File bigLicense = new File(rootDir, "BIG_LICENSE");
		if (bigLicense.exists())
		{
			bigLicense.delete();
		}

		do
		{
			FileUtils.writeStringToFile(bigLicense, content, true);
		}
		while (bigLicense.length() < fileSize);

		return bigLicense;
	}
}
