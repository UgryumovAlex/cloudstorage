На текущий момент реализовал на сервере аутентификацию, регистрацию, отключение пользователя и подключил команды с 2-3
ДЗ. Начал заниматься клиентом. На клиентской части реализовал браузер файлов. Начал реализовывать с клиента подключение к серверу.
Есть проблема с получение ответа от сервера.
...
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
...
    socket = new Socket(IP_ADDRESS, PORT);
    in = new DataInputStream(socket.getInputStream());
    out = new DataOutputStream(socket.getOutputStream());
...
/*Отправка и получение ответа*/
        try {
            out.write(String.format("auth %s %s", login, password).getBytes(StandardCharsets.UTF_8));
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

        String response = null;

        try {
            int size = in.available();
            StringBuffer buffer = new StringBuffer();
            for(int i = 0; i< size; i++) {
                buffer.append((char)in.read());
            }
            response = buffer.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }
...

Когда запускаю через трассировку в дебаггере, всё работает. Команда уходит и возвращается ответ.
Если просто запустить, то во входящем потоке на первый запрос будет пусто. А при отправке второй команды в
in будет ответ на первую команду.

На сервере ответ отправляю посредством "ctx.writeAndFlush".
