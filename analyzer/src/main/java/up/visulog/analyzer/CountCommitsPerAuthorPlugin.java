package up.visulog.analyzer;

import up.visulog.config.Configuration;
import up.visulog.gitrawdata.Commit;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CountCommitsPerAuthorPlugin implements AnalyzerPlugin {
    private final Configuration configuration;
    private Result result;

    public CountCommitsPerAuthorPlugin(Configuration generalConfiguration) {
        this.configuration = generalConfiguration;
    }

    public static Result processLog(List<Commit> gitLog) {
        var result = new Result();
        for (var commit : gitLog) {
            var nb = result.commitsPerAuthor.getOrDefault(commit.getAuthor(), 0);
            result.commitsPerAuthor.put(commit.getAuthor(), nb + 1);
        }
        return result;
    }

    @Override
    public void run() {
        result = processLog(Commit.parseLogFromCommand(configuration.getGitPath(), "log"));
    }

    @Override
    public Result getResult() {
        if (result == null) run();
        return result;
    }

    public static class Result implements AnalyzerPlugin.Result {
        private final Map<String, Integer> commitsPerAuthor = new HashMap<>();

        public Map<String, Integer> getCommitsPerAuthor() {
            return commitsPerAuthor;
        }

        @Override
        public String getResultAsString() {
            return commitsPerAuthor.toString();
        }

        @Override
        public String getResultAsHtmlDiv() {
            // <div><h1 onclick=\"toggle()\">Number of authors:</h1><div id=\"showDiv\">" + getResultAsString() +
            //                    "</div></div>
            StringBuilder html = new StringBuilder("<div> <h1 onclick=\"toggle('showDiv2')\">Number of commits per author:</h1><div id=\"showDiv2\"><ul>");
            for (var item : commitsPerAuthor.entrySet()) {
                String nom_mail = item.getKey();
                String nom = nom_mail.split("<")[0];
                String mail = nom_mail.split("<")[1].replaceAll(">", " ");
                html.append(String.format("<li><a href=\"mailto:" + mail + "\"> " + nom + "</a> : " + item.getValue() + "</li>"));
            }
            html.append("</ul></div></div>");
            return html.toString();
        }

        public String getRData() {
            StringBuilder R_txt = new StringBuilder();
            for (var item : commitsPerAuthor.entrySet()) {
                String nom_mail = item.getKey();
                String nom = nom_mail.split("<")[0];
                R_txt.append(String.format(nom + " " + item.getValue() + "\n"));
            }
            return R_txt.toString();
        }

        public FileReader getRtxt(String s, String lien) throws IOException {
                File txt = new File("commitsPA.txt");
                boolean append = txt.exists() ? false : true;
                FileOutputStream fos = new FileOutputStream(txt, append);
                fos.write(s.getBytes());
                fos.close();
                FileReader readerTxt = new FileReader(txt);
                return readerTxt;
        }

        public static void runWithR(String nomFichier)throws IOException {
            Process process = new ProcessBuilder("R", "CMD", "BATCH", nomFichier, "result.txt").start();
        }

    }
}

