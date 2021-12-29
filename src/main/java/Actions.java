import org.jutils.jprocesses.JProcesses;
import org.jutils.jprocesses.model.ProcessInfo;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.io.IOException;
import static com.codeborne.selenide.Selenide.open;


public class Actions {
    String currentFolder = System.getProperty("user.dir");

    ArrayList<String> inputCommand(String command) { //ввод команд через командную строку windows и получение набора строк с результатом выполнения
        ArrayList<String> result = new ArrayList<>();
        try {
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
            reader.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e2) {
            e2.printStackTrace();
        }
        return result;
    }

    void printResult(ArrayList<String> result){
        for (int i = 0; i < result.size(); i++) {
            System.out.println(result.get(i));
        }
    }

    void startTorService(){
        ProcessBuilder p = new ProcessBuilder();
        p.command(currentFolder+"\\tor-win32-0.4.6.9\\Tor\\tor.exe");
        try {
            p.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Boolean checkTorService() { //получить список процессов
        ArrayList<String> services = new ArrayList<String>();
        Boolean result = false;
        List<ProcessInfo> processesList = JProcesses.getProcessList();
        for (final ProcessInfo processInfo : processesList) {
            if(processInfo.getName().toLowerCase(Locale.ROOT).equals("tor.exe")){
                result=true;
            };
        }
        return result;
    }

    void restartTorService(){
            killTorService();
            startTorService();
    }

    void killTorService(){
        printResult(inputCommand("cmd /c \"chcp 65001 && taskkill /F /IM tor.exe /T"));
    }

    public void connectSelenide(String URL, Integer start, Integer end) {
        try {
            Thread.currentThread().sleep(start*1000);
        open(URL);
        Thread.currentThread().sleep(end*1000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}




