package up.visulog.gitrawdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Commit {

    // FIXME: (some of) these fields could have more specialized types than String
    private final String id;
    private final String date;
    private final String author;
    private final String description;
    private final String mergedFrom;

    
    public Commit(String id, String author, String date, String description, String mergedFrom) {
        this.id = id;
        this.author = author;
        this.date = date; 
        this.description = description;
        this.mergedFrom = mergedFrom;
    }

    public String getAuthor() {
        return author;
    }

    public String getId()
    {
        return this.id;
    }

    public String getDate() {
        return date;
    }

    // TODO: factor this out (similar code will have to be used for all git commands)
    public static List<Commit> parseLogFromCommand(Path gitPath, String command) {
        System.out.println("[Visulog] Parsing log from command...");
        long startTime = System.currentTimeMillis();
        ProcessBuilder builder = new ProcessBuilder("git", command).directory(gitPath.toFile());
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new RuntimeException("Error running git "+ command+". ", e);
        }
        InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        long endTime = System.currentTimeMillis();
        System.out.println("[Visulog] Commits parsed (" + String.valueOf(endTime - startTime) + "ms)");
        return parseLog(reader);
    }

    public static List<Commit> parseLog(BufferedReader reader) {
        var result = new ArrayList<Commit>();
        System.out.println("[Visulog] Parsing commits...");
        long startTime = System.currentTimeMillis();
        Optional<Commit> commit = parseCommit(reader);
        int i = 0 ;
        while (commit.isPresent()) {
            i++;
            //System.out.print(".");
            //if(i%40==0){
//                System.out.println();
  //          }
            result.add(commit.get());
            commit = parseCommit(reader);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("[Visulog] Parsed " + result.size() + " commits in " + String.valueOf(endTime - startTime) + "ms");
        return result;
    }

    /**
     * Parses a log item and outputs a commit object. Exceptions will be thrown in case the input does not have the proper format.
     * Returns an empty optional if there is nothing to parse anymore.
     */

    public static Optional<Commit> parseCommit(BufferedReader input) {
        try {
            var line = input.readLine();
            if (line == null) return Optional.empty(); // if no line can be read, we are done reading the buffer
            var idChunks = line.split(" ");
            if (!idChunks[0].equals("commit")) parseError();
            var builder = new CommitBuilder(idChunks[1]);

            line = input.readLine();
            while (!line.isEmpty()) {
                var colonPos = line.indexOf(":");
                var fieldName = line.substring(0, colonPos);
                var fieldContent = line.substring(colonPos + 1).trim();
                switch (fieldName) {
                    case "Author":
                        builder.setAuthor(fieldContent);
                        break;
                    case "Merge":
                        builder.setMergedFrom(fieldContent);
                        break;
                    case "Date":
                        builder.setDate(fieldContent);
                        break;
                    default:
                        throw new RuntimeException("Some field was ignored");
                }
                line = input.readLine(); //prepare next iteration
                if (line == null) parseError(); // end of stream is not supposed to happen now (commit data incomplete)
            }

            // now read the commit message per se
            var description = input
                    .lines() // get a stream of lines to work with
                    .takeWhile(currentLine -> !currentLine.isEmpty()) // take all lines until the first empty one (commits are separated by empty lines). Remark: commit messages are indented with spaces, so any blank line in the message contains at least a couple of spaces.
                    .map(String::trim) // remove indentation
                    .reduce("", (accumulator, currentLine) -> accumulator + currentLine); // concatenate everything
            builder.setDescription(description);
            return Optional.of(builder.createCommit());
        } catch (IOException e) {
            parseError();
        }
        return Optional.empty(); // this is supposed to be unreachable, as parseError should never return
    }

    // Helper function for generating parsing exceptions. This function *always* quits on an exception. It *never* returns.
    private static void parseError() {
        throw new RuntimeException("Wrong commit format.");
    }

    @Override
    public String toString() {
        return "Commit{" +
                "id='" + id + '\'' +
                (mergedFrom != null ? ("mergedFrom...='" + mergedFrom + '\'') : "") + //TODO: find out if this is the only optional field
                ", date='" + date + '\'' +
                ", author='" + author + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
