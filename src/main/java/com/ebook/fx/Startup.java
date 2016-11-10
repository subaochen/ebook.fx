/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebook.fx;

import com.ebook.fx.util.ImageCache;
import javafx.application.Application;
import javafx.stage.Stage;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

/**
 *
 * @author maykoone
 */
public class Startup extends Application {

    private Weld weld;

    @Override
    public void init() throws Exception {
        System.out.println("Initializing Weld...");
        weld = new Weld();
    }

    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("Initializing container...");
        WeldContainer container = weld.initialize();
        System.out.println("Firing start event...");
        container.event().select(Stage.class).fire(stage);
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Application stoping...");
        ImageCache.getInstance().clear();
        weld.shutdown();
    }

    public static void main(String[] args) {
        System.out.println("Launching application...");
        launch(args);
    }
}