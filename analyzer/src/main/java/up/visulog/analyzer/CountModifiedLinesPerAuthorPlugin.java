package up.visulog.analyzer;

import up.visulog.gitrawdata.Commit;
import up.visulog.gitrawdata.GetGitCommandOutput;
import up.visulog.config.Configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CountModifiedLinesPerAuthorPlugin implements AnalyzerPlugin
{
    private final Configuration configuration;
    private Result result;

    public CountModifiedLinesPerAuthorPlugin(Configuration config)
    {
        this.configuration = config;
    }

    public Result processLog(List<Commit> gitLog)
    {
        var res = new CountModifiedLinesPerAuthorPlugin.Result();
        Integer[] temp;
        String id1;
        String id2;

        for(int i=0; i<gitLog.size()-1; i++)
        {
            if(!res.modifiedLinesPerAuthor.containsKey(gitLog.get(i).getAuthor()))
            {
                temp = new Integer[2];
                temp[0] = Integer.valueOf(0);
                temp[1] = Integer.valueOf(0);
                res.modifiedLinesPerAuthor.put(gitLog.get(i).getAuthor(), temp);
            }

            id1 = gitLog.get(i+1).getId();
            id2 = gitLog.get(i).getId();
            Integer[] currentValues = res.modifiedLinesPerAuthor.get(gitLog.get(i).getAuthor());
            Integer[] addedValues = this.getLineDifference(id1, id2);
            temp = new Integer[2];
            temp[0] = Integer.valueOf(currentValues[0].intValue() + addedValues[0].intValue());
            temp[1] = Integer.valueOf(currentValues[1].intValue() + addedValues[1].intValue());
            res.modifiedLinesPerAuthor.replace(gitLog.get(i).getAuthor(), temp);
        }
        var result =  new CountModifiedLinesPerAuthorPlugin.Result();
        for (Map.Entry<String,Integer[]> item  : res.modifiedLinesPerAuthor.entrySet()){
            String tmp [] = item.getKey().split(" <");
            String nom = AuthorName(tmp[0]);
            if (! result.modifiedLinesPerAuthor.containsKey(nom)){
                result.modifiedLinesPerAuthor.put(nom,item.getValue());
            }
            else {
                Integer[] i = {result.modifiedLinesPerAuthor.get(nom)[0]+item.getValue()[0], result.modifiedLinesPerAuthor.get(nom)[1]+item.getValue()[1]};
                result.modifiedLinesPerAuthor.replace(nom,i);
            }
        }
        return result;
    }

    public Integer[] getLineDifference(String id1, String id2) 
    {
        String info = "";
        Integer[] res = new Integer[2];
        res[0] = Integer.valueOf(0);
        res[1] = Integer.valueOf(0);
        GetGitCommandOutput process = new GetGitCommandOutput(this.configuration.getGitPath(), "diff --shortstat "+id1+" "+id2);
        try {
            info = process.getOutput().readLine();
        } catch (IOException e) {
            throw new RuntimeException("Error running git diff. ", e);
        }
        if(info==null)
        {
            return res;
        }
        
        String[] temp = info.split(", ");
        if(temp.length==2)
        {
            if(temp[1].contains("insert"))
            {
                res[0] = Integer.valueOf(temp[1].split(" ")[0]);
            }
            else
            {
                res[1] = Integer.valueOf(temp[1].split(" ")[0]);
            }
        } 
        else
        {
            res[0] = Integer.valueOf(temp[1].split(" ")[0]);
            res[1] = Integer.valueOf(temp[2].split(" ")[0]);
        }        
        return res;
    }

    private static String AuthorName (String n){
        try {

            BufferedReader reader = new BufferedReader(new FileReader("../AuthorName.txt"));
            String ligne;
            while((ligne = reader.readLine()) != null){
                String[] name = ligne.split("=");
                for(int i = 0;i < name.length;i++){
                    if (name[i].equals(n)) return name[0];
                }
            }
            return n;
        } catch (Exception ex){
            System.err.println("[Visulog] ! Error (AuthorName) ");
        }
        return "";
    }

    @Override
    public void run()
    {
        long startTime=System.currentTimeMillis();
        this.result = this.processLog(Commit.parseLogFromCommand(this.configuration.getGitPath(),"log"));
        System.out.println("[Visulog] Thread of ModifiedLinesPerAuthor plugin obtained in " + (System.currentTimeMillis()-startTime)/1000 +"s");
    }

    @Override
    public Result getResult()
    {
        if(this.result==null)
            this.run();
        return this.result;
    }


    public static class Result implements AnalyzerPlugin.Result 
    {
        private final HashMap<String,Integer[]> modifiedLinesPerAuthor = new HashMap<>();

        public HashMap<String,Integer[]> getResultAsHashMap()
        {
            return modifiedLinesPerAuthor;
        }

        public String getResultAsString()
        {
            String res = "";
            for(Map.Entry<String,Integer[]> i:modifiedLinesPerAuthor.entrySet())
            {
                res = res + i.getKey() +" : "
                          + i.getValue()[0].toString() + " Added lines, "
                          + i.getValue()[1].toString() + " Deleted lines."
                          + "\n";
            }
            return res;
        }

        @Override
        public String getResultAsHtmlDiv() 
        {
            
            StringBuilder html = new StringBuilder("<div><h1 onclick=\"toggle('showDiv6')\">Modified lines per author: </h1>");
            if(modifiedLinesPerAuthor.isEmpty()) 
            {
                return html.append("None</div>").toString();
            }
            html.append(" <div id=\"showDiv6\"  style =\"display:none;\"><table><tbody><thead><tr><th>Author</th><th>Added</th><th>deleted lines</th></thead>");

            for (Entry<String,Integer[]> item : modifiedLinesPerAuthor.entrySet()) {
                if(item!=null) 
                {
                    html.append("<tr>");
                    html.append("<td>").append(item.getKey()).append("</td>");
                    html.append("<td>").append(item.getValue()[0].toString()).append("</td>");
                    html.append("<td>").append(item.getValue()[1].toString()).append("</td>");
                    html.append("</tr>");
                }
            }
            html.append("</tbody></table></div></div>");
            return html.toString();
        }

        @Override
        public String getRData() {
            // TODO
            return null;
        }

        @Override
        public void CreateRtxt(String s, String lien) throws IOException {
            // TODO
        }

    }
}
