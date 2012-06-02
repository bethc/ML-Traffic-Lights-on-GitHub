/*
COMP9417 Machine Learning
Major Project - Traffic Lights Reinforcement Learning
Beth Crane
Gill Morris
Nathan Wilson
 */

import interfaces.Car;
import interfaces.LearningModule;
import interfaces.RoadMap;
import interfaces.TrafficLight;
import utils.Coords;
import java.util.ArrayList;
import java.util.List;

public class Main
{
    public static void main (String[] args)
    {
        //(do we need to take in any arguments? my thought is perhaps we
        //    should save the learned values to a file and pass that 
        //    file as an argument if we wish to resume from a previous 
        //    trial)
        //
        //         - probably. though I'd rather see how inefficient it
        //           is to make the poor thing learn everything again
        //           each time it runs before worrying about it. -- Gill

    	int runTime = 1000100;
        int quietTime = 1000000;
        boolean graphicalOutput = true;
        boolean consoleOutput = false;

        //Initialise map, list of cars currently on map, and list of 
        //trafficlights
        RoadMap map = new RoadMapImpl();
        List<Car> cars = new ArrayList<Car>();
        List<TrafficLight> trafficLights = 
                new ArrayList<TrafficLight>();
        trafficLights.add(
                //FIXME: s|20|gridSize/2 but thats in RoadMap
                //    - no, we arent always going to have one traffic
                //      light in the map centre. when we have 4 for
                //      example
                //
                //      - sure, but then how do you calculate *their*
                //        positions? perhaps have lights at
                //        {gridSize/4, gridSize/4} and {120, -9443}?
                new TrafficLightImpl(new Coords(20,20),false));
        double trafficDensityThreshold = 0.4;
        LearningModule learningModule = new LearningModuleImpl();
        Viewer v = new Viewer();

        //Basic logic for each time step
        // - change traffic lights if required - call a function from 
        //   'learning' class to do this
        // - move cars in their current direction by velocity (modify 
        //   velocity if necessary - using CarAI)
        // - spawn cars at extremities
        // - Now that we have the new state, update the qvalue for the p
        //   revious s,a pair
        
        // TODO: no longer matters how small i make my font,
        // this loop ain't gonna fit on my screen
        // or even robert's
        for (int timeToRun = 0; timeToRun < runTime; ++timeToRun) {
            RoadMap currentState = map.copyMap();
            currentState.addCars(cars);
            List<Integer> switchedLights;
            List<Integer> states = new ArrayList<Integer>();
            List<Integer> nextStates = new ArrayList<Integer>();
            List<Integer> rewards = new ArrayList<Integer>();

            // Update the traffic lights - switch or stay
            //Get integer representing state BEFORE cars are moved
            //and lights are switched
            for (TrafficLight light: trafficLights) {
                states.add(currentState.stateCode(light));
            }
            //returns a list of true/false that the lights were 
            //switched for learning purposes
            switchedLights = learningModule.updateTrafficLights(
                    currentState, trafficLights
            );
            RoadMap nextState = currentState.copyMap();

            //Move cars currently on map
            List<Car> carsToRemove = new ArrayList<Car>();
            for (Car car : cars) {
                // FIXME: assumes map contains a single light 
                // (will fix when we add lights)
                car.updateVelocity(trafficLights.get(0), currentState);
                car.updatePosition();
                if (car.hasLeftMap(map)) {
                     carsToRemove.add(car);
                }
            }
            cars.removeAll(carsToRemove);

            //Spawn cars onto map extremities
            for (Coords roadEntrance : map.getRoadEntrances())
            {
                if (
                    Math.random() <= trafficDensityThreshold &&
                    !currentState.carAt(roadEntrance)
                ) {
                    // TODO: if currentState.carAt(roadEntrance) we
                    // should probably model that there's a queue
                    // outside the map and/or fail our traffic light
                    // learner
                    Car c = new CarImpl(
                            new Coords(roadEntrance),
                            map.getStartingVelocity(roadEntrance)
                    );
                    cars.add(c);
                }
            }
            nextState.addCars(cars);

            for (TrafficLight light : trafficLights) {
                rewards.add(learningModule.reward(nextState,light));
                nextStates.add(nextState.stateCode(light));
            }
            //calculate reward and state code for each traffic light
            //To learn we need to pass through - previous states, 
            //actions taken, rewards
            learningModule.learn(
                    states, switchedLights, rewards, 
                    nextStates, trafficLights
            );
            if (timeToRun >= quietTime) {
                if (graphicalOutput) {
                    v.view(map, cars, trafficLights);
                }
                if (consoleOutput) {
                    map.print(cars, trafficLights);
                }
                try {
                    Thread.sleep(100);
                } catch (Exception e) {}
            }
            for (TrafficLight light : trafficLights) {
                light.clock();
            }
        }
    }
}
