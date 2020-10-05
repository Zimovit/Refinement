package org.example;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.math.BigInteger;

public class App extends Application
{
    private static final int MATERIAL_COST = 25000;
    private static final int IMPROVED_MATERIAL_COST = 125000;
    private static final int REFINEMENT_COST = 10000;
    private static int itemPrice;
    private static double chanceToRefine = 0.5, chanceToBrake = 0.25;
    private static int numberOfTries = 1000000;
    private static int[][] safeRefinementTable ={{1, 5, 100000}, {2, 10, 220000}, {3, 15, 470000}, {4, 25, 910000}, {6, 50, 1630000}, {10, 85, 2740000}};

    public static void main( String[] args )
    {
        Application.launch(args);

    }

    public void start(Stage stage){

        //начнём с верхней панели
        final TextField price = new TextField("0"), numberOfTries = new TextField(String.valueOf(this.numberOfTries));
        Label priceLabel = new Label("Цена шмотки:"), numberOfTriesLabel = new Label("Количество попыток:");
        Button calculateButton = new Button("Рассчитать!");
        //TODO прицепить к кнопке рассчёты
        HBox upperPane = new HBox(5, priceLabel, price, numberOfTriesLabel, numberOfTries, calculateButton);

        //теперь пилим табличку
        //TODO запилить таки табличку
        final Label[][]labels = new Label[7][4]; //6 строк, 4 столбца в таблице
        //Инициализация столбцов
        for (int i = 5; i <= 10; i++){
            labels[i-5][0] = new Label("заточка на +10 с безопасной заточкой на +" + i);
            labels[i-5][2] = new Label("На +15");
            labels[i-5][1] = new Label("0");
            labels[i-5][3] = new Label("0");
        }
        labels[6][0] = new Label("Полностью небезопасная");
        labels[6][2] = new Label("На +15");
        labels[6][1] = new Label("0");
        labels[6][3] = new Label("0");

        //Добавляем элементы в табличку
        GridPane table = new GridPane();
        for (int row = 0; row < 7; row++){
            for (int column = 0; column < 4; column++){
                Label label = labels[row][column];
                label.setPadding(new Insets(5));
                table.add(label, column, row);
            }
        }
        table.setPadding(new Insets(10));
        table.setGridLinesVisible(true);

        //Сообщение об ошибке, если юзверь косячит
        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText("Нужно ввести целые положительные числа, а не какую-то фигню.");

        //корневой узел
        BorderPane root = new BorderPane(table);
        root.setTop(upperPane);

        //цепляем к кнопке листенер
        calculateButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                int tries;

                try{
                    itemPrice = Integer.parseInt(price.getText().trim());
                    tries = Integer.parseInt(numberOfTries.getText().trim());
                }
                catch (NumberFormatException e){
                    alert.show();
                    return;
                }

                if (itemPrice <= 0 || tries <= 0) {
                    alert.show();
                    return;
                }
                //точка до +4
                int refinementPrice = itemPrice + MATERIAL_COST*4 + REFINEMENT_COST*10;

                for (int i = 0; i < 6; i++){
                    BigInteger unsafeMediumCost = new BigInteger("0");
                    for (int j = 0; j < tries; j++){
                        unsafeMediumCost = unsafeMediumCost.add(new BigInteger(String.valueOf(unsafeRefinement(i+5, 10))));

                    }
                    unsafeMediumCost = unsafeMediumCost.divide(new BigInteger(String.valueOf(tries)));
                    int cost = refinementPrice + safeRefinement(i+5) + unsafeMediumCost.intValue();
                    labels[i][1].setText(String.valueOf(cost));
                    unsafeMediumCost = new BigInteger("0");
                    for (int j = 0; j < tries; j++){
                        unsafeMediumCost = unsafeMediumCost.add(new BigInteger(String.valueOf(improvedRefinement())));
                    }
                    unsafeMediumCost = unsafeMediumCost.divide(new BigInteger(String.valueOf(tries)));
                    cost = cost+unsafeMediumCost.intValue();
                    labels[i][3].setText(String.valueOf(cost));
                }

                BigInteger unsafeMediumCost = new BigInteger("0");
                for (int j = 0; j < tries; j++){
                    unsafeMediumCost = unsafeMediumCost.add(new BigInteger(String.valueOf(unsafeRefinement(4, 10))));

                }
                unsafeMediumCost = unsafeMediumCost.divide(new BigInteger(String.valueOf(tries)));
                int cost = refinementPrice + unsafeMediumCost.intValue();
                labels[6][1].setText(String.valueOf(cost));

                unsafeMediumCost = new BigInteger("0");
                for (int j = 0; j < tries; j++){
                    unsafeMediumCost = unsafeMediumCost.add(new BigInteger(String.valueOf(improvedRefinement())));
                }
                unsafeMediumCost = unsafeMediumCost.divide(new BigInteger(String.valueOf(tries)));

                labels[6][3].setText(String.valueOf(cost+unsafeMediumCost.intValue()));

            }
        });

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Рассчёт заточки");
        stage.show();

    }


    private static int unsafeRefinement(int startRefinement, int finalRefinement) throws IllegalArgumentException {
        //нужно проверить, есть ли смысл точиться
        if (startRefinement == finalRefinement) return 0;   //уже заточено
        if ((startRefinement > finalRefinement)) throw new IllegalArgumentException("Начальный уровень выше целевого");

        //и вообще верные ли параметры

        if (startRefinement < 4 || finalRefinement > 10) throw new IllegalArgumentException("Неверно указаны уровни заточки");

        int cost = 0;   //эта переменная будет хранить стоимость заточки от начальной до желаемой.

        boolean isBroken = false;  //отдаём в заточку целую вещь

        //refLevel - на этот уровень пытаемся точиться
        for (int refLevel = startRefinement+1; refLevel <= finalRefinement;){
            //платим за материалы и за заточку
            cost = cost + MATERIAL_COST + (REFINEMENT_COST * refLevel);

            if (isBroken) {
                cost = cost + itemPrice;  //чинимся
                isBroken = false;   //и починились
            }

            boolean success = Math.random() < chanceToRefine;  //пан или пропал
            if (success){
                //точнулись, оплата уже прошла, просто повышаем уровень и на норвый цикл
                refLevel++;
            } else {
                //а тут всё фигово. Для начала слетает заточка на 1
                if (refLevel > 4) refLevel--;

                //а ведь можно ещё и сломаться
                if (Math.random() < chanceToBrake) isBroken = true;  //хрусть

            }
        }

        return cost;

    }

    private static int safeRefinement(int goalLevel) throws IllegalArgumentException {
        //проверим корректность аргумента
        if (goalLevel < 5 || goalLevel > 10) throw new IllegalArgumentException("Неверно указан уровень заточки");

        int cost = 0;   //здесь собираем стоимость
        int iterNumber = goalLevel-5; //побежим по табличке, заточка на +5 - нулевая строка таблицы
        for (int i = 0; i <= iterNumber; i++){
            cost = cost + itemPrice*safeRefinementTable[i][0] + MATERIAL_COST*safeRefinementTable[i][1] + safeRefinementTable[i][2];
        }
        return cost;
    }

    private static int improvedRefinement(){
        int cost = 0;

        for (int i = 0; i < 5;){
            cost += 225000;
            if (Math.random() < chanceToRefine){
                i++;
            } else {
                i--;
                if (i < 0) {
                    cost = cost + unsafeRefinement(9, 10);
                    i = 0;
                }
                cost += itemPrice;
            }
        }
        return cost;
    }
}
