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

        //Initialise map, list of cars currently on map, and list of 
        //trafficlights
    	
    	int runTime = 1001000;
    	
        RoadMap map = new RoadMapImpl();
        List<Car> cars = new ArrayList<Car>();
        List<TrafficLight> trafficLights = 
                new ArrayList<TrafficLight>();
        trafficLights.add(
                //FIXME: s|20|gridSize/2 but thats in RoadMap
                new TrafficLightImpl(new Coords(20,20),false));
        double trafficDensityThreshold = 0.4;
        LearningModule learningModule = new LearningModuleImpl();

        //Basic logic for each time step
        // - change traffic lights if required - call a function from 
        //   'learning' class to do this
        // - move cars in their current direction by velocity (modify 
        //   velocity if necessary - using CarAI)
        // - spawn cars at extremities
        // Now that we have the new state, update the qvalue for the previous s,a pair
        for (int timeToRun = 0; timeToRun < runTime; ++timeToRun) {
            RoadMap currentState = map.copyMap();
            RoadMap nextState = map.copyMap();
            currentState.addCars(cars);

            // Update the traffic lights - switch or stay
            learningModule.updateTrafficLights(
                currentState, trafficLights);

            //Move cars currently on map
            List<Car> carsToRemove = new ArrayList<Car>();
            for (Car car : cars)
            {
                // FIXME: assumes map contains a single light
                car.updateVelocity(trafficLights.get(0), currentState);
                car.updatePosition();
                if (car.hasLeftMap(map))
                {
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
                    cars.add(new CarImpl
                    (
                        new Coords(roadEntrance),
                        map.getStartingVelocity(roadEntrance)
                    ));
                }
            }

            nextState.addCars(cars);
            
            // Updates q-values
            learningModule.learn(currentState, nextState, trafficLights);
            for (TrafficLight light : trafficLights) {
                light.clock();
            }

            //Learns 1000000 time steps and then lets us watch it
            if (timeToRun > 1000000) {
	            map.print(cars, trafficLights);
	            try {
	                Thread.sleep(1000);
	            }
	            catch (InterruptedException e) {
	                e.printStackTrace();
	            }
            }
        }
    }
}
