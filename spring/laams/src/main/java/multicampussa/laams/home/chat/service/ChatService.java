package multicampussa.laams.home.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import multicampussa.laams.home.chat.domain.ChatRoom;
import multicampussa.laams.home.chat.repository.ChatRepository;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private Map<String, ChatRoom> chatRooms;
    private final ChatRepository chatRepository;

    @PostConstruct
    //의존관계 주입완료되면 실행되는 코드
    private void init() {
        chatRooms = new LinkedHashMap<>();
    }

    //채팅방 불러오기
    public List<ChatRoom> findAllRoom() {
        //채팅방 최근 생성 순으로 반환
        List<ChatRoom> result = chatRepository.findAll();
        Collections.reverse(result);

        return result;
    }

    //채팅방 하나 불러오기
    public ChatRoom findById(String roomId) {
        return chatRepository.findByRoomId(roomId);
    }

    //채팅방 생성
    public ChatRoom createRoom(String name) {
        ChatRoom chatRoom = ChatRoom.create(name);
//        chatRooms.put(chatRoom.getRoomId(), chatRoom);
        return chatRepository.save(chatRoom);
    }
}