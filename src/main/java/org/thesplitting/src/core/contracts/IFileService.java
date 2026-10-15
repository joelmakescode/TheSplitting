package org.thesplitting.src.core.contracts;

import java.io.IOException;
import java.nio.file.Path;

public interface IFileService {
    /**
     * Creates the dedicated folder directory.
     * @param folderName
     * @throws IOException
     */
    void folderCreate(Path folderName) throws IOException;

    /**
     * Creates a dedicated json file.
     * @param folderName
     * @param fileName
     * @throws IOException
     */
    void jsonFileCreate(Path folderName, String fileName, Object input);

    /**
     * Reads the jsonFile and returns a JsonObject
     * @param folderName
     * @param fileName
     * @return JsonObject
     */
    <T> T jsonFileRead(Path folderName, String fileName, Class<T> clazz);

    /**
     * Receives an input and writes it into a specified .json file.
     * @param folderName where the .json file is.
     * @param fileName name of the .json file.
     * @param input that is going to be changed.
     */
    void jsonFileWrite(Path folderName, String fileName, Object input);
}
