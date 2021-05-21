package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
	public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}


	private final class MyGameState implements GameState {

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;



		private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining, final ImmutableList<LogEntry> log, final Player mrX, final List<Player> detectives) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

			if (setup.rounds.isEmpty()) throw new IllegalArgumentException("Rounds is empty!"); // throw if no rounds in setup
			if (mrX == null) throw new NullPointerException("No MrX"); // throw if mrx not provided
			if (detectives == null) throw new NullPointerException(("No detectives")); // throw if no detectives provided
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Empty graph"); // throw if no nodes on graph
			if (mrX.isDetective()) throw new IllegalArgumentException("MrX is a detective"); // throw if mrx is classified as a detective

			List<Piece> usedPieces = new ArrayList<>();
			List<Integer> detectiveSpawnLocations = new ArrayList<>();
			for (Player detective : detectives) {
				Piece detectivePiece = detective.piece();
				if (usedPieces.contains(detectivePiece)) { // go through each detective and make sure not already added
					throw new IllegalArgumentException("Duplicate detectives");
				}
				usedPieces.add(detectivePiece);

				if (detectivePiece.isMrX()) { // make sure detectives aren't classified as mrx
					throw new IllegalArgumentException("MrX in detectives");
				}

				Integer detectiveSpawnLocation = detective.location();
				if (detectiveSpawnLocations.contains(detectiveSpawnLocation)) { // make sure detectives are spawning in different places
					throw new IllegalArgumentException("Duplicate detective spawn locations");
				}
				detectiveSpawnLocations.add(detectiveSpawnLocation);


				ImmutableMap<ScotlandYard.Ticket, Integer> detectiveTickets = detective.tickets();
				for (ScotlandYard.Ticket detectiveTicketType : detectiveTickets.keySet()) { // make sure detectives don't have any mrx tickets
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
			List<Piece> allPieces = new ArrayList<>();
			allPieces.add(mrX.piece());
			for (Player i : detectives) { // add the piece for each detective to allPieces
				allPieces.add(i.piece());
			}
			return ImmutableSet.copyOf(allPieces);
		}

		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (Player i : detectives) { // return location if detective, else empty
				if (i.piece().equals(detective)) return Optional.of(i.location());
			}
			return Optional.empty();
		}

		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			if (!(getPlayers().contains(piece))) { // make sure requested piece is indeed a player
				return Optional.empty();
			}
			if (piece.isMrX()) { // if mrx, can just return his tickets
				return Optional.of(new MyTicketBoard(mrX.tickets()));
			} else if (piece.isDetective()) { // else find which detective it is, and return their tickets
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
			getAvailableMoves();

			return this.winner;

		}

		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			List<Move> allMoves = new ArrayList<Move>();
			Set<Piece> newWinner = new HashSet<Piece>();

			Set<Piece> detectiveWinners = new HashSet<>();
			for (Player j : detectives){ // initialise with the piece types of all playing detectives (stores all winning detectives if they succeed)
				detectiveWinners.add(j.piece());
			}

			if(log.size() == setup.rounds.size() && remaining.contains(mrX.piece())) {
				newWinner.add(mrX.piece());
			} // check if mrX already escaped

			for(Player i : detectives){
				if(i.location() == mrX.location()){
					newWinner.addAll(detectiveWinners);
					break;
				}
			} // check if any detective catches mrX (i.e. on same node)

			List<Move> allmovesofDec = new ArrayList<>();
			for(Player i : detectives){
				allmovesofDec.addAll(makeSingleMoves(setup,detectives,i,i.location()));
			} // get all possible moves for the detectives
			if(allmovesofDec.isEmpty()){  // check if all detectives got stuck (i.e. have no moves)
				newWinner.add(mrX.piece());
			}
			if(newWinner.isEmpty()) { // if no one has won
				int roundleft = setup.rounds.size(); // num of rounds left
				if(remaining.contains(mrX.piece())){ // if mrx remains
					if(roundleft >= 2){ // if more than two rounds then can do single and double moves
						allMoves.addAll(makeSingleMoves(setup,detectives,mrX, mrX.location()));
						allMoves.addAll(makeDoubleMoves(setup,detectives,mrX,mrX.location()));
					} // otherwise can only do single moves
					else if(roundleft == 1) allMoves.addAll(makeSingleMoves(setup,detectives,mrX,mrX.location()));
					if(allMoves.isEmpty())  { // if mrx can't move, detectives win
						newWinner.addAll(detectiveWinners);
					}
				}
				else {
					for(Player i : detectives){
						if(remaining.contains(i.piece())) { // add moves for each remaining detective
							allMoves.addAll(makeSingleMoves(setup, detectives, i, i.location()));
						}
					}
				}
			}

			this.winner = ImmutableSet.copyOf(newWinner);

			List<Move> emptyMove = new ArrayList<>();
			if(!winner.isEmpty()) return ImmutableSet.copyOf(emptyMove); // if there is a winner then return empty moves
			moves = ImmutableSet.copyOf(allMoves); // else return the moves calculated

			return moves;
		}



		@Override
		public GameState advance(Move move) {
			if(!getAvailableMoves().contains(move)) throw new IllegalArgumentException("Illegal move: "+move); // make sure proposed move is in the set of valid moves

			Piece currentPlayer = move.commencedBy();
			List<Player> newDetectives = new ArrayList<>();
			List<Piece> newRemaining = new ArrayList<>();
			List<LogEntry> newLog = new ArrayList<>();
			newRemaining.addAll(remaining);
			newLog.addAll(log);
			int des;
			if (move instanceof Move.SingleMove) { // get destination if single move made
				des = ((Move.SingleMove) move).destination;
			}
			else { // get destination if double move made
				des = ((Move.DoubleMove) move).destination2;
			}

			newRemaining.remove(move.commencedBy()); // remove whoever made the move
			if (currentPlayer.isMrX()) {
				mrX = mrX.use(move.tickets()).at(des); // use relevant tickets
				newDetectives.addAll(detectives);
				newRemaining.remove(mrX.piece()); // remove mrx from remaining
				for(Player i : detectives){ // add all detective pieces to remaining, since current player is mrx
					newRemaining.add(i.piece());
				}

				for(ScotlandYard.Ticket i : move.tickets()){
					if (!i.equals(ScotlandYard.Ticket.DOUBLE)) { // if not double, reveal
						if (setup.rounds.get(newLog.size())) {
							newLog.add(LogEntry.reveal(i, mrX.location()));
						}
						else {
							newLog.add(LogEntry.hidden(i));
						}
					}
				}
			} else {
				for (Player i : detectives) {
					if (i.piece().equals(currentPlayer)) {
						newDetectives.add(i.use(move.tickets()).at(des)); // make the detective's move
						mrX = mrX.give(move.tickets()); // give mrx his tickets
					}
					else newDetectives.add(i); // add detectives that don't currently have their go
				}

				// skip the remaining detective players if they got stuck
				List<Move> remainingMoves = new ArrayList<>();
				for(Player i : detectives){
					if(newRemaining.contains(i.piece())) {
						remainingMoves.addAll(makeSingleMoves(setup, detectives, i, i.location())); // added all the moves of detectives that haven't moved
					}
				}
				if(!newRemaining.isEmpty() && remainingMoves.isEmpty()) { // if the remaining detectives cannot move, skip to next round
					newRemaining.clear();
					newRemaining.add(mrX.piece());
				}

				if(newRemaining.isEmpty()) {
					newRemaining.add(mrX.piece()); // added mrX to start a new round
				}

			}
			return new MyGameState(setup,ImmutableSet.copyOf(newRemaining),ImmutableList.copyOf(newLog),mrX,ImmutableList.copyOf(newDetectives));

		}

	}

	private static ImmutableSet<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		final var singleMoves = new ArrayList<Move.SingleMove>();

		for (int destination : setup.graph.adjacentNodes(source)) { // go through every node
			boolean isOccupied = false;
			boolean hasTicket = false;
			for (Player detective : detectives) {
				if (detective.location() == destination) {
					isOccupied = true;
					break; // try next detective
				}
			}
			if (isOccupied) {
				continue; // try next location as someone is here already
			}
			for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
				if (player.has(t.requiredTicket())) hasTicket = true;
				if (hasTicket) { // add relevant single move if they have required ticket
					singleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
				}
				if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 1)) { // add relevant secret single move if they have secret ticket
					singleMoves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
				}
			}
		}
		return ImmutableSet.copyOf(singleMoves);
	}

	private static ImmutableSet<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		final var doubleMoves = new ArrayList<Move.DoubleMove>();

			if (player.isMrX() && player.hasAtLeast(ScotlandYard.Ticket.DOUBLE, 1)) { // make single move then see which moves can be made once that move is made, hence getting double moves
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
