import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static final Map<String, String> GCODES = new HashMap<>();

    static {
        GCODES.put("xZeroPosition", "X0");
        GCODES.put("yZeroPosition", "Y0");
        GCODES.put("zZeroPosition", "Z0");
        GCODES.put("zeroPosition", "X0Y0Z0");
        GCODES.put("xyZeroPosition", "X0Y0");
        GCODES.put("xzZeroPosition", "X0Z0");
        GCODES.put("yzZeroPosition", "Y0Z0");
        GCODES.put("pause", "M00");
        GCODES.put("tongsOn", "M03");
        GCODES.put("limitX","M04");
        GCODES.put("tongsOff", "M05");
        GCODES.put("valvuleOn", "M07");
        GCODES.put("valvuleOff", "M09");
        GCODES.put("endProgram", "M30");
        GCODES.put("zDown", "Z-29.000");
        GCODES.put("zUp", "Z-20.000");
        GCODES.put("zZero", "Z0.000");
        GCODES.put("tableFinal", "G1X3000F40000");
    }

    static Scanner sc = new Scanner(System.in);
    public static void main(String[] args) {

        start();
    }


    public static void start () {

        while (true) {
            String fileName = askForFile();
            Path path = getFile(fileName);

            List<String> linesOriginalFile = readFile(path);

            if (linesOriginalFile == null) {
                continue;
            }

            double xOrigin = askForXOrigin();
            double yOrigin = askForYOrigin();

            List<String> linesNewFile = cleanList(linesOriginalFile, xOrigin, yOrigin);
            List<String> linesDefinitive = createNewList(linesNewFile);
            createNewFile(fileName, linesDefinitive);

            break;
        }
    }


    public static String askForFile(){

        System.out.println("¿Qué archivo  quieres convertir?");
        String name = sc.nextLine();

        return name + ".txt";
    }


    public static Path getFile (String name) {
        Path path = Paths.get(name);

        return path;
    }


    public static double askForXOrigin(){
        System.out.println("¿Qué distancia tiene el tope en X?");
        double xOrigin = sc.nextDouble();
        sc.nextLine();

        xOrigin = xOrigin - 18;

        return xOrigin;
    }


    public static double askForYOrigin(){
        System.out.println("¿Qué distancia tiene el tope en Y?");
        double yOrigin = sc.nextDouble();
        sc.nextLine();

        yOrigin = yOrigin - 20.5;

        return yOrigin;
    }


    public static List<String> readFile (Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                System.out.println(line);
            }
            return lines;
        } catch (IOException e) {
            System.out.println("Ese archivo no existe.");
            return null;
        }
    }


    public static List<String> cleanList(List<String> lines, double xOrigin, double yOrigin) {

        List<String> newLines = new ArrayList<>();

        for (String line : lines) {
            char looked = 'X';
            int beginsInt = line.indexOf(looked);
            if (beginsInt != -1) {
                System.out.println(beginsInt);
                String originalChain = line.substring(beginsInt);
                System.out.println(originalChain);
                String[] chain = originalChain.split(" ");
                String[] chainX = chain[0].split("=");
                String[] chainY = chain[1].split("=");
                String measureX = convertToFinalMeasure(chainX[1], xOrigin);
                String measureY = convertToFinalMeasure(chainY[1], yOrigin);
                String definitive = chainX[0] + measureX + chainY[0] + measureY;
                newLines.add(definitive);
            }
            else {
                continue;
            }
        }


        //ordenamos la trayectoria por X y luego por Y
        newLines.sort((a, b) -> {
            double x1 = getX(a);
            double x2 = getX(b);

            if (x1 != x2) {
                return Double.compare(x1, x2);
            }

            double y1 = getY(a);
            double y2 = getY(b);

            return Double.compare(y1, y2);
        });

        return newLines;
    }

    public static List<String> createNewList(List<String> lines) {

        List<String> definitiveList = new ArrayList<>();

        definitiveList.add(gCodeOrdersGetter("xzZeroPosition"));
        definitiveList.add(gCodeOrdersGetter("pause"));
        definitiveList.add(gCodeOrdersGetter("tongsOn"));
        definitiveList.add(gCodeOrdersGetter("limitX"));
        definitiveList.add(gCodeOrdersGetter("pause"));
        definitiveList.add(gCodeOrdersGetter("tongsOff"));

        for (String line : lines) {
            definitiveList.add(line);
            definitiveList.add(gCodeOrdersGetter("zDown"));
            definitiveList.add(gCodeOrdersGetter("valvuleOn"));
            definitiveList.add(gCodeOrdersGetter("zUp"));
            definitiveList.add(gCodeOrdersGetter("valvuleOff"));
        }

        definitiveList.add(gCodeOrdersGetter("zZeroPosition"));
        definitiveList.add(gCodeOrdersGetter("tongsOff"));
        definitiveList.add(gCodeOrdersGetter("tableFinal"));
        definitiveList.add(gCodeOrdersGetter("xyZeroPosition"));
        definitiveList.add(gCodeOrdersGetter("endProgram"));

        for (String line : definitiveList) {
            System.out.println(line);
        }

        return definitiveList;
    }


    public static void createNewFile(String name, List<String> lines) {
        String[] chainName = name.split("\\.");
        String newName = chainName[0] + "Grapas.txt";

        Path file = Paths.get(newName);
        System.out.println(newName);

        try{
            Files.write(file, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("El archivo " + newName + " ha sido creado con éxito.");
        }
        catch (IOException e) {
            System.out.println("Error al intentar guardar el archivo " + e.getMessage());
        }
    }


    public static String convertToFinalMeasure(String number, double origin) {

        double convertedNumber = Double.parseDouble(number);
        double finalMeasure = convertedNumber + origin;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("0.###", symbols);
        //String finalMeasureChain = String.valueOf(df.format(finalMeasure));

        return df.format(finalMeasure);
    }


    public static double getX(String line) {

        int start = line.indexOf("X") + 1;
        int end = line.indexOf("Y");

        String xString = line.substring(start, end);

        return Double.parseDouble(xString);
    }


    public static double getY(String line) {

        int start = line.indexOf("Y") + 1;

        String yString = line.substring(start);

        return Double.parseDouble(yString);
    }


    public static String gCodeOrdersGetter(String key) {

        return GCODES.get(key);
    }


}