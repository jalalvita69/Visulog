package up.visulog.analyzer;

import up.visulog.config.Configuration;
import up.visulog.gitrawdata.GetGitCommandOutput;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;

public class CountCommitsPerHourPlugin implements AnalyzerPlugin{
    private final Configuration configuration;
    private Result result;

    public CountCommitsPerHourPlugin(Configuration generalConfiguration) {
        this.configuration = generalConfiguration;
    }
    private static String pwd;
    static {
        pwd = RInvocation.pwd();
    }

    @Override
    public void run() {
        long startTime=System.currentTimeMillis();
        result =  aux();
        RInvocation invoke = new RInvocation();
        invoke.RGene(result,pwd+"/CommitsPerHour.R");
        System.out.println("[Visulog] Thread of CommitsPerHour plugin obtained in " + (System.currentTimeMillis()-startTime)/1000 +"s");
    }

    private Result aux(){
        Result r0 = new Result();
        // On execute étape par étape la commande :
        // git log --date=local --pretty=format:%ad | cut -d\  -f4 | cut -d\: -f1 | sort | uniq -c
        var output = new GetGitCommandOutput(configuration.getGitPath(),
                "log --pretty=format:%ad"
        );
        r0.commitsPerHour = new LinkedList<String>();
        try {
            LinkedList<Integer> list_temp = new LinkedList<>();
            var getting = output.getOutput();

            String s = "";
            while ((s = getting.readLine()) != null) {
                String[] cut1 = s.split(" "); // premier cut
                String cut2 = cut1[3].split(":")[0]; // second cut
                list_temp.add(Integer.parseInt(cut2));
            }
            Collections.sort(list_temp); // sort
            String st = "";
            for(int i = 0 ; i < list_temp.size() ; i++) { // uniq + count
                if(!st.contains(list_temp.get(i).toString())) {
                    int c = 0;
                    st += list_temp.get(i);
                    for (Integer integer : list_temp) {
                        if (list_temp.get(i).intValue() == integer.intValue()) c++;
                    }
                    r0.commitsPerHour.add(c + " " + list_temp.get(i).toString());
                }
            }
            getting.close();
        } catch (IOException ignored) { // ignored car rendra une liste null
        }
        return r0;
    }


    @Override
    public Result getResult() {
        if (result == null) run();
        return result;
    }

    public static class Result implements AnalyzerPlugin.Result {
        private LinkedList<String> commitsPerHour = new LinkedList<>();

        public LinkedList<String> getCommitsPerHour() {
            return commitsPerHour;
        }

        @Override
        public String getResultAsString() {
            return commitsPerHour.toString();
        }

        @Override
        public String getResultAsHtmlDiv() {
            StringBuilder html = new StringBuilder("<div><h1 onclick=\"toggle('showDiv5')\">Commits Per Hour: </h1>");
            if(commitsPerHour.isEmpty()) return html.append("No commit</div>").toString();
            html.append(" <div id=\"showDiv5\"  style =\"display:none;\"><embed src=\""+ pwd + "/.graphs/CommitsPerHour.pdf\"width=\"500\" height=\"400\"><table><tbody><thead><tr><th>Hour</th><th>Commits count</th><th>Proportion</th></thead>");

            int c = 0;
            for(String item : commitsPerHour) {
                c+=Integer.parseInt(item.split(" ")[0]);
            }
            for (String item : commitsPerHour) {
                if(item!=null) {
                    html.append("<tr>");
                    String howMany = item.split(" ")[0];
                    String when = item.split(" ")[1]+" h";
                    html.append("<td>").append(when).append("</td>");
                    html.append("<td>").append(howMany).append("</td>");
                    double percent =(double)Integer.parseInt(howMany)/c*100;
                    int rounded = (int)percent;
                    if(Math.abs(percent-(int)percent)>0.5) rounded=(int)percent+1;
                    html.append("<td>").append(rounded).append(" %</td>");
                    html.append("</tr>");
                }
            }
            html.append("</tbody></table></div></div>");
            return html.toString();
        }

        @Override
        public void CreateRtxt(String s, String lien) throws IOException {
            File txt = new File(lien + "/commitsPerHour.txt" );
            boolean append = !txt.exists();
            FileOutputStream fos = new FileOutputStream(txt, append);
            fos.write(s.getBytes());
            fos.close();
        }

        @Override
        public String getRData() {
            StringBuilder R_txt = new StringBuilder();
            for (var item : commitsPerHour) {
                String nombreDeCommits = item.split(" ")[0];
                String heure = item.split(" ")[1]+"h";
                R_txt.append(nombreDeCommits + " "  + heure + "\n");
            }
            return R_txt.toString();
        }
    }
}
