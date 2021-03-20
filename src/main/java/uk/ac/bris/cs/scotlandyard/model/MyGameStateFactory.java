package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.ArrayList;
import java.util.HashSet;
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
	private ImmutableSet<Piece> winner = ImmutableSet.copyOf(new HashSet<>());


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
		if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Empty graph");
		List<Piece> usedPieces = new ArrayList<>();
		List<Integer> detectiveSpawnLocations = new ArrayList<>();
		for (Player detective : detectives) {
			Piece detectivePiece = detective.piece();
			if (usedPieces.contains(detectivePiece)) {
				throw new IllegalArgumentException("Duplicate detectives");
			}
			usedPieces.add(detectivePiece);

			if (detectivePiece.isMrX()) {
				throw new IllegalArgumentException("MrX in detectives");
			}

			Integer detectiveSpawnLocation = detective.location();
			if (detectiveSpawnLocations.contains(detectiveSpawnLocation)) {
				throw new IllegalArgumentException("Duplicate detective spawn locations");
			}
			detectiveSpawnLocations.add(detectiveSpawnLocation);

			if (mrX.isDetective()) {
				throw new IllegalArgumentException("MrX is a detective");
			}

			ImmutableMap<ScotlandYard.Ticket, Integer> detectiveTickets = detective.tickets();
			for (ScotlandYard.Ticket detectiveTicketType : detectiveTickets.keySet()) {
				Integer detectiveTicketCount = detectiveTickets.get(detectiveTicketType);
				if (detectiveTicketType == ScotlandYard.Ticket.SECRET && detectiveTicketCount > 0) {
					throw new IllegalArgumentException("Detective(s) have a secret ticket");
				}
				if (detectiveTicketType == ScotlandYard.Ticket.DOUBLE && detectiveTicketCount > 0) {
					throw new IllegalArgumentException("Detective(s) have a double ticket");
				}
			}
		}
	}



	@Override public GameSetup getSetup(){ return setup; }
	@Override public ImmutableSet<Piece> getPlayers() {
		// not sure if we should be using "remaining" here or if the detectives set will update?
		List<Piece> allPieces = new ArrayList<>();
		for (Player i : detectives) {
			allPieces.add(i.piece());
		}
		allPieces.add(mrX.piece());
		return ImmutableSet.copyOf(allPieces);
	}
	@Override public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
		for (Player i : detectives) {
			if (i.piece().equals(detective))  return Optional.of(i.location());
		}
		return Optional.empty();
	}
	@Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
		// I think we need to code TicketBoard before this will pass?
		if (!(getPlayers().contains(piece))) {
			return Optional.empty();
		}
		if (piece.isMrX()) {
			return Optional.of(new MyTicketBoard(mrX.tickets()));
		}
		else if (piece.isDetective()) {
			for (Player i : detectives) {
				if (i.piece().equals(piece)) {
					return Optional.of(new MyTicketBoard(i.tickets()));
				}
			}
		}
		return Optional.empty();
	}
	@Override public ImmutableList<LogEntry> getMrXTravelLog() { return log; }
	@Override public ImmutableSet<Piece> getWinner(){ return winner; }
	@Override public ImmutableSet<Move> getAvailableMoves() {
		// shouldn't this onlu return moves available for the current player?e
		List<Move> allMoves = new ArrayList<Move>();
		allMoves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
		for (Player i : detectives) {
			allMoves.addAll(makeSingleMoves(setup, detectives, i, i.location()));
		}
		System.out.println(ImmutableSet.copyOf(allMoves));
		return ImmutableSet.copyOf(allMoves);
	}
	@Override public GameState advance(Move move) {
		if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
		return null;
	}

}

	private static ImmutableSet<Move.SingleMove> makeSingleMoves(
			GameSetup setup,
			List<Player> detectives,
			Player player,
			int source){
		final var singleMoves = new ArrayList<Move.SingleMove>();

		for(int destination : setup.graph.adjacentNodes(source)) {
			boolean isoccupied = false;
			boolean hasticket = false;
			for (Player detective : detectives){
				if (detective.location() == destination) {
					isoccupied = true;
					if (isoccupied) {
						continue; // try next destination
					}
				}
			}
			for(ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source,destination,ImmutableSet.of())) {
				if(player.has(t.requiredTicket())) hasticket = true;
				if(!isoccupied && hasticket){
					singleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(),destination));
				}
			}
			if(source == 194 && player.has(ScotlandYard.Ticket.SECRET)) singleMoves.add(new Move.SingleMove(player.piece(),source, ScotlandYard.Ticket.SECRET,157));
			else if (source == 157 && player.hasAtLeast(ScotlandYard.Ticket.SECRET,1)) {
				singleMoves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, 194));
				singleMoves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, 115));
			}
			else if (source == 115 && player.hasAtLeast(ScotlandYard.Ticket.SECRET,1)) {
				singleMoves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, 157));
				singleMoves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, 108));
			}
			else if (source == 108 && player.hasAtLeast(ScotlandYard.Ticket.SECRET,1)) {
				singleMoves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, 115));
			}
		}
		return ImmutableSet.copyOf(singleMoves);
	}

}


