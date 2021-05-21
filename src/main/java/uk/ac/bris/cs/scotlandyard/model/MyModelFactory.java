package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;
import java.util.Set;


/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {
	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {

		MyGameStateFactory myGame = new MyGameStateFactory();
		Board.GameState myGamestate = myGame.build(setup,mrX,detectives);

		return new myModel(myGamestate);
	}
	final class myModel implements Model{
		private Board.GameState myGamestate;
		private Set<Observer> Observers = new HashSet<>();

		private myModel(final Board.GameState myGamestate) {
			this.myGamestate = myGamestate; // initialise the game state
		}
		public Board getCurrentBoard() {
			return this.myGamestate; // get the current game board
		}

		public void registerObserver(Observer observer) {
			if(observer == null) throw new NullPointerException(); // check it exists then register an observer
			if(Observers.contains(observer)) throw new IllegalArgumentException(); // make sure the registered observer does not already exist
			this.Observers.add(observer);
		}

		public void unregisterObserver(Observer observer) {
			if(observer == null) throw new NullPointerException(); // check it exists then unregister an observer
			if(!Observers.contains(observer)) throw new IllegalArgumentException();// make sure the registered observer does not already exist
			this.Observers.remove(observer);
		}

		@Nonnull
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(this.Observers); // return the current observers that are registered
		}

		public void chooseMove(@Nonnull Move move) {
			myGamestate = myGamestate.advance(move);
			Set<Observer> Observers = getObservers();
			Board newBoard = getCurrentBoard();
			// first check if there's a winner
			if(myGamestate.getWinner().isEmpty()) {
				for(Observer i : Observers){ // if not over, register moves made
					i.onModelChanged(newBoard, Observer.Event.MOVE_MADE); // update the move that has been made
				}
			}
			else{
				for(Observer i : Observers){ // if over, register game over
					i.onModelChanged(newBoard, Observer.Event.GAME_OVER); // someone has won the game, game over
				}
			}
		}
	}
}
