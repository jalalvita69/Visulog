package up.visulog.analyzer;

import org.junit.Test;
import org.w3c.dom.Document;
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
        //var res = CountCommitsPerAuthorPlugin.processLog(log);
        //RInvocation invoke = new RInvocation();
        //invoke.RGene(res, "CommitsPerAuthor.R");



        }
    }
