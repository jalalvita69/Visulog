package up.visulog.analyzer;

import org.junit.Test;
import up.visulog.gitrawdata.Commit;
import up.visulog.gitrawdata.CommitBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class TestCountCommitsPerAuthorPlugin {

    /* Let's check whether the number of authors is preserved and that the sum of the commits of each author is equal to the total number of commits */
    @Test
    public void checkCommitSum() {
        var log = new ArrayList<Commit>();
        String[] authors = {"foo", "bar", "baz"};
        var entries = 20;
        for (int i = 0; i < entries; i++) {
            log.add(new CommitBuilder("").setAuthor(authors[i % 3]).createCommit());
        }
        var res = CountCommitsPerAuthorPlugin.processLog(log);
        //System.out.println(res.getRtxt());
        assertEquals(authors.length, res.getCommitsPerAuthor().size());
        var sum = res.getCommitsPerAuthor().values()
                .stream().reduce(0, Integer::sum);
        assertEquals(entries, sum.longValue());
    }

    public static void main(String[] args) throws IOException {
        var log = new ArrayList<Commit>();
        String[] authors = {"foo", "bar", "baz"};
        var entries = 20;
        for (int i = 0; i < entries; i++) {
            log.add(new CommitBuilder("").setAuthor(authors[i % 3]).createCommit());
        }
        var res = CountCommitsPerAuthorPlugin.processLog(log);
        //System.out.println(res.getRData());
        String line;
        BufferedReader b = new BufferedReader(res.getRtxt(res.getRData(), ));
        while((line = b.readLine()) != null){
            System.out.println(line);
        }
        try {
            mkdir(".visulogRTempFiles");
            //CREER LE FICHIER .txt ICI avec: String lien = pwd() + "/.visulogRTempFiles"
            //runWithR("commitsPA.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    }
