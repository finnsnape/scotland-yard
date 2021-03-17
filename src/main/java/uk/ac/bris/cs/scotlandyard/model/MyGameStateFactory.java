package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		// TODO
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);

	}


private final class MyGameState implements GameState {

	private GameSetup setup;
	private ImmutableSet<Piece> remaining;
	private ImmutableList<LogEntry> log;
	private Player mrX;
	private List<Player> detectives;
	private ImmutableList<Player> everyone;
	private ImmutableSet<Move> moves;
	private ImmutableSet<Piece> winner;

	private MyGameState(
			final GameSetup setup,
			final ImmutableSet<Piece> remaining,
			final ImmutableList<LogEntry> log,
			final Player mrX,
			final List<Player> detectives
	)
	{
		this.setup = setup;
		this.remaining = remaining;
		this.log = log;
		this.mrX = mrX;
		this.detectives = detectives;
		if(setup.rounds.isEmpty()) throw new IllegalArgumentException("Rounds is empty!");


		if(mrX == null) throw new NullPointerException("No MrX");
		if(detectives == null) throw new NullPointerException(("No detectives"));
		List<Piece> usedPieces = new ArrayList<>();
		for (Player detective : detectives) {
			Piece detectivePiece = detective.piece();
			usedPieces.add(detectivePiece);
			if (usedPieces.contains(detectivePiece)) {
				throw new IllegalArgumentException("Duplicate detectives");
			}
		}


		//
	}



	@Override public GameSetup getSetup(){ return setup; }
	@Override public ImmutableSet<Piece> getPlayers() { return remaining; }
	@Override public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
		for (Player i : detectives) {
			if (i.piece().equals(detective))  return Optional.of(i.location());
		}
		return Optional.empty();

	}
	@Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {

		return Optional.empty();
	}
	@Override public ImmutableList<LogEntry> getMrXTravelLog() { return log; }
	@Override public ImmutableSet<Piece> getWinner(){ return null; }
	@Override public ImmutableSet<Move> getAvailableMoves() {

		return null; }
	@Override public GameState advance(Move move) {
		if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
		return null;
	}

}}
