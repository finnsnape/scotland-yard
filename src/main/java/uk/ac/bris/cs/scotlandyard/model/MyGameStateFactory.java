package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull
	@Override
	public GameState build(
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
		private Boolean mrXstuck = false;


		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives
		) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

			if (setup.rounds.isEmpty()) throw new IllegalArgumentException("Rounds is empty!");
			if (mrX == null) throw new NullPointerException("No MrX");
			if (detectives == null) throw new NullPointerException(("No detectives"));
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


		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@Override
		public ImmutableSet<Piece> getPlayers() {
			// not sure if we should be using "remaining" here or if the detectives set will update?
			List<Piece> allPieces = new ArrayList<>();
			allPieces.add(mrX.piece());
			for (Player i : detectives) {
				allPieces.add(i.piece());
			}
			return ImmutableSet.copyOf(allPieces);
		}

		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (Player i : detectives) {
				if (i.piece().equals(detective)) return Optional.of(i.location());
			}
			return Optional.empty();
		}

		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			// I think we need to code TicketBoard before this will pass?
			if (!(getPlayers().contains(piece))) {
				return Optional.empty();
			}
			if (piece.isMrX()) {
				return Optional.of(new MyTicketBoard(mrX.tickets()));
			} else if (piece.isDetective()) {
				for (Player i : detectives) {
					if (i.piece().equals(piece)) {
						return Optional.of(new MyTicketBoard(i.tickets()));
					}
				}
			}
			return Optional.empty();
		}

		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Override
		public ImmutableSet<Piece> getWinner() {
			Set<Piece> newWinner = new HashSet<>();

			Set<Piece> detectivewinners = new HashSet<>();

			for (Player j : detectives){
				detectivewinners.add(j.piece());
			}
			if(log.size() == setup.rounds.size() && remaining.contains(mrX.piece())) {
				newWinner.add(mrX.piece());
				return ImmutableSet.copyOf(newWinner);
			}
			for(Player i : detectives){
				if(i.location() == mrX.location()){
					newWinner.addAll(detectivewinners);
					break;
				}
			}
			if(remaining.contains(mrX.piece())){
				if(getAvailableMoves().isEmpty()) {
					newWinner.addAll(detectivewinners);
					mrXstuck = true;
				}

			}
			
			System.out.println(newWinner);
			winner = ImmutableSet.copyOf(newWinner);
			return winner;

		}

		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			List<Move> allMoves = new ArrayList<Move>();
			if(mrXstuck) return ImmutableSet.copyOf(allMoves);
			//if(!getWinner().isEmpty() ) return ImmutableSet.copyOf(allMoves);


			int roundleft = setup.rounds.size();
			if(remaining.contains(mrX.piece())){
				if(roundleft >= 2){
					allMoves.addAll(makeSingleMoves(setup,detectives,mrX, mrX.location()));
					allMoves.addAll(makeDoubleMoves(setup,detectives,mrX,mrX.location()));
				}
				else if(roundleft == 1) allMoves.addAll(makeSingleMoves(setup,detectives,mrX,mrX.location()));
			}
			else{
				for(Player i : detectives){
					if(remaining.contains(i.piece())) {
						allMoves.addAll(makeSingleMoves(setup, detectives, i, i.location()));
					}
				}
			}
			moves = ImmutableSet.copyOf(allMoves);
			System.out.println(moves);
			return moves;
		}



		@Override
		public GameState advance(Move move) {
			if(!getAvailableMoves().contains(move)) throw new IllegalArgumentException("Illegal move: "+move);

			Piece currentPlayer = move.commencedBy();
			List<Player> newdetectives = new ArrayList<>();
			List<Piece> newremaining = new ArrayList<>();
			List<LogEntry> newlog = new ArrayList<>();
			newremaining.addAll(remaining);
			newlog.addAll(log);
			Integer des = move.visit(new Move.Visitor<>(){
				@Override public Integer visit(Move.SingleMove singleMove){
					return singleMove.destination;
				}
				@Override public Integer visit(Move.DoubleMove doubleMove){
					return doubleMove.destination2;
				}
			});

			if (currentPlayer.isMrX()) {
				mrX = mrX.use(move.tickets()).at(des);
				newdetectives.addAll(detectives);
				newremaining.remove(mrX.piece());
				for(Player i : detectives){
					newremaining.add(i.piece());
				}

				for(ScotlandYard.Ticket i : move.tickets()){
					if (!i.equals(ScotlandYard.Ticket.DOUBLE)) {
						if(setup.rounds.get(newlog.size())) newlog.add(LogEntry.reveal(i, mrX.location()));
						else newlog.add(LogEntry.hidden(i));
					}
				}
			} else {
				newremaining.remove(move.commencedBy());
				for (Player i : detectives) {
					if (i.piece().equals(currentPlayer)) {
						newdetectives.add(i.use(move.tickets()).at(des));
						mrX = mrX.give(move.tickets());
					}
					else newdetectives.add(i);
				}
				if(newremaining.isEmpty()) newremaining.add(mrX.piece());

			}

			return new MyGameState(setup,ImmutableSet.copyOf(newremaining),ImmutableList.copyOf(newlog),mrX,ImmutableList.copyOf(newdetectives));

		}

	}

	private static ImmutableSet<Move.SingleMove> makeSingleMoves(
			GameSetup setup,
			List<Player> detectives,
			Player player,
			int source) {
		final var singleMoves = new ArrayList<Move.SingleMove>();

		for (int destination : setup.graph.adjacentNodes(source)) {
			boolean isoccupied = false;
			boolean hasticket = false;
			for (Player detective : detectives) {
				if (detective.location() == destination) {
					isoccupied = true;
					break; // try next detective
				}
			}
			if (isoccupied) {
				continue; // try next location
			}
			for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
				if (player.has(t.requiredTicket())) hasticket = true;
				if (hasticket) {
					singleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
				}
				if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 1)) {
					singleMoves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
				}
			}
		}
		return ImmutableSet.copyOf(singleMoves);
	}

	private static ImmutableSet<Move.DoubleMove> makeDoubleMoves(
			GameSetup setup,
			List<Player> detectives,
			Player player,
			int source
	) {
		final var doubleMoves = new ArrayList<Move.DoubleMove>();

			if (player.isMrX() && player.hasAtLeast(ScotlandYard.Ticket.DOUBLE, 1)) {
				for (Move.SingleMove i : makeSingleMoves(setup, detectives, player, source)) {
					for (Move.SingleMove j : makeSingleMoves(setup, detectives, player, i.destination)) {
						if (i.ticket == j.ticket) {
							if (!player.hasAtLeast(i.ticket, 2))
								continue;
						}
						doubleMoves.add(new Move.DoubleMove(player.piece(), source, i.ticket, i.destination, j.ticket, j.destination));
					}
				}
			}

		return ImmutableSet.copyOf(doubleMoves);
	}
}


