package edu.smith.cs.csc212.fishgrid;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class manages our model of gameplay: missing and found fish, etc.
 * @author jfoley
 *
 */
public class FishGame {
	/**
	 * This is the world in which the fish are missing. (It's mostly a List!).
	 */
	World world;
	/**
	 * The player (a Fish.COLORS[0]-colored fish) goes seeking their friends.
	 */
	Fish player;
	/**
	 * The home location.
	 */
	FishHome home;
	/**
	 * These are lost fish that move more frequently.
	 */
	Fish fastScared;
	/**
	 * These are the missing fish!
	 */
	List<Fish> missing;
	/**
	 * These are fish we've found!
	 */
	List<Fish> found;
	/**
	 * These are fish we've brought home!
	 */
	List<Fish> homeFish;
	/**
	 * The heart location!
	 */
	Heart heart;

	List<Heart> hearts;

	public static final int NUM_ROCKS = 8;
	public static final int NUM_FALLING_ROCKS = 10;
	public static final int NUM_HEARTS = 1;
	public static final int NUM_BUBBLES = 5;

	/**
	 * Number of steps!
	 */
	int stepsTaken;

	/**
	 * Score!
	 */
	int score;

	/**
	 * Create a FishGame of a particular size.
	 * @param w how wide is the grid?
	 * @param h how tall is the grid?
	 */
	public FishGame(int w, int h) {
		world = new World(w, h);

		missing = new ArrayList<Fish>();
		found = new ArrayList<Fish>();
		homeFish = new ArrayList<Fish>();
		hearts = new ArrayList<Heart>();

		// Add a home!
		home = world.insertFishHome();

		// Add falling rocks!
		for (int i=0; i<NUM_FALLING_ROCKS; i++) {
			world.insertFallingRockRandomly();
		}

		// Add bubbles!
		for (int i=0; i<NUM_BUBBLES; i++) {
			world.insertBubbleRandomly();
		}
		// Add rocks!
		for (int i=0; i<NUM_ROCKS; i++) {
			world.insertRockRandomly();

		}
		// Add a snail!
		world.insertSnailRandomly();

		// Make the player out of the 0th fish color.
		player = new Fish(0, world);
		// Start the player at "home".
		player.setPosition(home.getX(), home.getY());
		player.markAsPlayer();
		world.register(player);

		// Generate fish of all the colors but the first into the "missing" List.
		for (int ft = 1; ft < Fish.COLORS.length; ft++) {
			Fish friend = world.insertFishRandomly(ft);
			missing.add(friend);
		}		
	}

	/**
	 * How we tell if the game is over: if missingFishLeft() == 0.
	 * @return the size of the missing list.
	 */
	public int missingFishLeft() {
		return missing.size();
	}

	/**
	 * This method is how the Main app tells whether we're done.
	 * @return true if the player has won (or maybe lost?).
	 */
	public boolean gameOver() {

		// bringing fish to fishhome
		if(this.player.inSameSpot(this.home)) {

			homeFish.addAll(found);

			for(WorldObject fish : found) {
				world.remove(fish);
			}
			for(WorldObject fish: homeFish) {
				found.remove(fish);
			}
		}

		// remove homeFish from the missing list
		for(Fish fish1: homeFish) {
			missing.remove(fish1);
		}
		// missing fish can find their way home
		for(Fish fish: missing) {

			if(fish.inSameSpot(this.home)) {
				homeFish.add(fish);
				world.remove(fish);
			}	
		}

		return missing.isEmpty() && found.isEmpty();
	}


	/**
	 * Update positions of everything (the user has just pressed a button).
	 */
	public void step() {
		// Keep track of how long the game has run.
		this.stepsTaken += 1;

		// Make sure missing fish *do* something.
		wanderMissingFish();

		// Starting at the second following fish, increase bored and when it gets to 20 steps
		// remove add it to missing
		for(int i=1; i<found.size(); i++ ) {
			Fish fish = found.get(i);
			fish.bored+= 1; 
			if(fish.bored > 20) {
				missing.add(fish);
			}

		}

		// removing found fish from missing and set bored back to 0
		for(Fish fish1: missing) {
			found.remove(fish1);
			fish1.bored = 0;
		}

		// These are all the objects in the world in the same cell as the missing fish.
		for(Fish fishes: missing) {
			List<WorldObject> sameLocation = fishes.findSameCell();
			// The missing fish is there, too, let's skip them.
			sameLocation.remove(fishes);
			// when fish are in the same cell as hearts remove hearts
			for (WorldObject wo: sameLocation) {
				if(wo instanceof Heart) {
					wo.remove();
				}
			}
		}

		// These are all the objects in the world in the same cell as the player.
		List<WorldObject> overlap = this.player.findSameCell();
		// The player is there, too, let's skip them.
		overlap.remove(this.player);

		// if the player collects a heart
		for (WorldObject wo : overlap) {
			if(wo instanceof Heart) {
				score += 1;
				wo.remove();
			}
		}

		// If we find a fish, remove it from missing.
		for (WorldObject wo : overlap) {
			// It is missing if it's in our missing list.
			if (missing.contains(wo)) {
				if (!(wo instanceof Fish)) {
					throw new AssertionError("wo must be a Fish since it was in missing!");
				}
				// Convince Java it's a Fish (we know it is!)
				Fish justFound = (Fish) wo;

				found.add(justFound);

				missing.remove(justFound);

				// Increase score when you find a fish!

				if (justFound.getColor() == Color.magenta) {
					score += 50;
				} else {
					score += 10;
				}	
			}

		}

		// Thank You to Yutong who gave me this idea
		// when randDouble is 0.0, make a heart show up
		Random rand = ThreadLocalRandom.current();
		if (rand.nextDouble() < 0.1) {
			world.insertHeartRandomly();

		}

		// When fish get added to "found" they will follow the player around.
		World.objectsFollow(player, found);
		// Step any world-objects that run themselves.
		world.stepAll();
	}

	private void wanderMissingFish() {
		Random rand = ThreadLocalRandom.current();

		for (Fish lost : missing) {	
			// TA Grace helped me
			// for fastScared fish when the random Double is 0.0-0.7 inclusive make the fish move 
			if(lost.fastScared) {
				if (rand.nextDouble() < 0.8) {
					lost.moveRandomly(); 
				}
			} else { 
				// make non fastScared fish move about 3/10 of the time
				if (rand.nextDouble() < 0.3) { 
					lost.moveRandomly(); 
				} 
			}
		}
	}

	/**
	 * This gets a click on the grid. We want it to destroy rocks that ruin the game.
	 * @param x - the x-tile.
	 * @param y - the y-tile.
	 */
	public void click(int x, int y) {
		
		System.out.println("Clicked on: "+x+","+y+ " world.canSwim(player,...)="+world.canSwim(player, x, y));
		List<WorldObject> atPoint = world.find(x, y);

		// remove rocks when user clicks on them
		for (WorldObject wo: atPoint) {
			if(wo.isRock()) {
				wo.remove();
			}
		}
		// remove bubbles when user clicks on them (doesn't include player fish)
		for (WorldObject wo: atPoint) {
			if(wo.isBubble()) {
				wo.remove();
			}
		}
	}
}
