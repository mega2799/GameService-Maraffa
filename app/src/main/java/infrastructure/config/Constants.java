package infrastructure.config;

import application.config.AppConstants;

/**
 * Infrastructure constants: HTTP routes, methods, and tags.
 * JSON attribute keys are in {@link AppConstants}.
 */
public class Constants {
	// Re-export application constants for backward compatibility within infrastructure
	public static final String USERNAME = AppConstants.USERNAME;
	public static final String GAME_ID = AppConstants.GAME_ID;
	public static final String NUMBER_OF_PLAYERS = AppConstants.NUMBER_OF_PLAYERS;
	public static final String CARD_VALUE = AppConstants.CARD_VALUE;
	public static final String CARD_SUIT = AppConstants.CARD_SUIT;
	public static final String TRICK = AppConstants.TRICK;
	public static final String CALL = AppConstants.CALL;
	public static final String ENDED = AppConstants.ENDED;
	public static final String EXPECTED_SCORE = AppConstants.EXPECTED_SCORE;
	public static final String GAME = AppConstants.GAME;
	public static final String STATUS = AppConstants.STATUS;
	public static final String GAME_MODE = AppConstants.GAME_MODE;
	public static final String IS_SUIT_FINISHED = AppConstants.IS_SUIT_FINISHED;
	public static final String COINS_4_USERNAME = AppConstants.COINS_4_USERNAME;
	public static final String TEAM = AppConstants.TEAM;
	public static final String GUIID = AppConstants.GUIID;
	public static final String POSITION = AppConstants.POSITION;
	public static final String AUTHOR = AppConstants.AUTHOR;
	public static final String PASSWORD = AppConstants.PASSWORD;
	public static final String START_ATTR = AppConstants.START_ATTR;
	public static final String JOIN_ATTR = AppConstants.JOIN_ATTR;
	public static final String NOT_FOUND = AppConstants.NOT_FOUND;
	public static final String FULL = AppConstants.FULL;
	public static final String MESSAGE = AppConstants.MESSAGE;
	public static final String ALREADY_JOINED = AppConstants.ALREADY_JOINED;
	public static final String ILLEGAL_TRUMP = AppConstants.ILLEGAL_TRUMP;
	public static final String TRUMP = AppConstants.TRUMP;
	public static final String SUIT = AppConstants.SUIT;
	public static final String PLAY = AppConstants.PLAY;
	public static final String DECK = AppConstants.DECK;
	public static final String INVALID = AppConstants.INVALID;
	public static final String NOT_ALLOWED = AppConstants.NOT_ALLOWED;
	public static final String TURN = AppConstants.TURN;
	public static final String RESULT = AppConstants.RESULT;
	public static final String VALUE = AppConstants.VALUE;
	public static final String ERROR = AppConstants.ERROR;
	public static final String GUEST = AppConstants.GUEST;
	public static final String TOTAL = AppConstants.TOTAL;
	public static final String NEW_GAME_CREATION = AppConstants.NEW_GAME_CREATION;
	public static final String CLOSED = AppConstants.CLOSED;

	// routes
	public static final String CREATE_GAME = "game/create";
	public static final String JOIN_GAME = "game/join";
	public static final String START_GAME = "game/start";
	public static final String PLAYER_CARDS = "game/:" + GAME_ID + "/:" + USERNAME + "/cards";
	public static final String PLAY_CARD = "round/playCard";
	public static final String CAN_START = "round/canStart/:" + GAME_ID;
	public static final String CHOOSE_TRUMP = "round/chooseTrump";
	public static final String START_NEW_ROUND = "round/startNewRound";
	public static final String CHANGE_TEAM = "game/changeTeam";
	public static final String STATE = "game/state/:" + GAME_ID;
	public static final String CARDS_ON_HAND = "round/cardsOnHand/:" + GAME_ID;
	public static final String CARDS_ON_TABLE = "round/cardsOnTable/:" + GAME_ID;
	public static final String END_ROUND = "round/end/:" + GAME_ID;
	public static final String END_GAME = "game/end/:" + GAME_ID;
	public static final String MAKE_CALL = "round/makeCall";
	public static final String GAMES = "game/getGames";
	public static final String GETGAME = "game/:" + GAME_ID;
	public static final String COINS_4 = "game/4Coins/:" + GAME_ID + "/username:" + COINS_4_USERNAME;
	public static final String GET_PLAYERS = "player";
	public static final String NEW_GAME = "game/newGame";
	public static final String SET_PASSWORD = "game/password";
	public static final String REMOVE_USER = "game/remove";
	public static final String GET_TOTAL_GAMES = "game/count";
	public static final String CHAT = "chat";
	public static final String NOTIFY = "game/notify";

	// methods
	public static final String PLAYERS_METHOD = "GET";
	public static final String CREATE_GAME_METHOD = "POST";
	public static final String NEW_GAME_METHOD = "POST";
	public static final String JOIN_GAME_METHOD = "PATCH";
	public static final String START_GAME_METHOD = "PATCH";
	public static final String PLAY_CARD_METHOD = "POST";
	public static final String CAN_START_METHOD = "GET";
	public static final String GET_PLAYER_CARD_METHOD = "GET";
	public static final String CHOOSE_TRUMP_METHOD = "POST";
	public static final String START_NEW_ROUND_METHOD = "PATCH";
	public static final String CHANGE_TEAM_METHOD = "PATCH";
	public static final String STATE_METHOD = "GET";
	public static final String CARDS_ON_HAND_METHOD = "GET";
	public static final String CARDS_ON_TABLE_METHOD = "GET";
	public static final String END_METHOD = "GET";
	public static final String MAKE_CALL_METHOD = "POST";
	public static final String GAMES_METHOD = "GET";
	public static final String COINS_4_METHOD = "GET";
	public static final String GET_TOTAL_GAMES_METHOD = "GET";
	public static final String EXIT_GAME = "DELETE";
	public static final String PASSOWRD_METHOD = "PATCH";
	public static final String REMOVE_USER_METHOD = "PATCH";

	// tags
	public static final String GAME_TAG = "Middleware.Game";
	public static final String ROUND_TAG = "Middleware.Round";

	// game constants (delegated to domain)
	public static final int NUMBER_OF_CARDS = 40;
	public static final int MARAFFA_SCORE = 9;
	public static final int ELEVEN_ZERO_SCORE = 11;
}
