package love.dreamscale.flow.activity


class InMemoryMessageLogger implements MessageLogger {

	List<Object> messages = []

	void flush() {
		messages.clear()
	}

	@Override
	void writeMessage(Long taskId, Object message) {
		messages.add(message)
	}

}
