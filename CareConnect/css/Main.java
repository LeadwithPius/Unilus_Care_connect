import java.util.Scanner;
public class Main{
    public static double calculateDeliveryCost(double weight){
          double totalcost = 25 + (4 * weight);

          if (totalcost > 150){
            totalcost = totalcost * 0.9;
          }

          return totalcost;

       }public static void main(String[] args){
        System.out.println("Enter the weight");
        Scanner scanner = new Scanner(System.in);
        double weight = scanner.nextDouble();

        double cost = calculateDeliveryCost(weight);
        System.out.println("The total delivery cost is K" + cost);


}}