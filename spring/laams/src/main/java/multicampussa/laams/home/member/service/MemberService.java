package multicampussa.laams.home.member.service;

import lombok.RequiredArgsConstructor;
import multicampussa.laams.config.RedisUtil;
import multicampussa.laams.director.domain.Director.Director;
import multicampussa.laams.home.member.dto.*;
import multicampussa.laams.home.member.jwt.JwtTokenProvider;
import multicampussa.laams.home.member.repository.MemberDirectorRepository;
import multicampussa.laams.home.member.repository.MemberManagerRepository;
import multicampussa.laams.manager.domain.centerManager.CenterManager;
import multicampussa.laams.manager.domain.centerManager.CenterManagerRepository;
import multicampussa.laams.manager.domain.manager.Manager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberDirectorRepository memberDirectorRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JavaMailSender javaMailSender;
    private final MemberManagerRepository memberManagerRepository;
    private final CenterManagerRepository centerManagerRepository;
    private final RedisUtil redisUtil;
    private final Random random = new SecureRandom();

    // 회원가입
    public ResponseEntity<String> signUp(MemberSignUpDto memberSignUpDto) {
        boolean centerManagerBoolean = false;
        List<CenterManager> centerManagers = centerManagerRepository.findAll();
        for (CenterManager centerManager : centerManagers) {
            if (centerManager.getCode().equals(memberSignUpDto.getCenterManagerCode())) {
                centerManagerBoolean = true;
            }
        }

        if (memberSignUpDto.getCenterManagerCode().equals("")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("센터 담당자 코드를 입력하세요.");
        }

        if (!centerManagerBoolean) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("센터 담당자 코드가 일치하지 않습니다.");
        }

        if ((memberDirectorRepository.existsByEmail(memberSignUpDto.getEmail()) && !memberDirectorRepository.findByEmail(memberSignUpDto.getEmail()).get().getIsDelete())
                || memberManagerRepository.existsByEmail(memberSignUpDto.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 존재하는 이메일입니다.");
        }

        if (memberDirectorRepository.existsById(memberSignUpDto.getId())
                && !memberDirectorRepository.findById(memberSignUpDto.getId()).get().getIsDelete()
                || memberManagerRepository.existsById(memberSignUpDto.getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 존재하는 아이디입니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(memberSignUpDto.getPw());
        Director newDirector = redisUtil.get(memberSignUpDto.getEmail(), Director.class);
        if (newDirector == null || !newDirector.getIsVerified()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이메일 인증을 진행해주세요.");
        } else {
            // 삭제한 이메일로 다시 한 번 회원가입 할 때
            if (memberDirectorRepository.existsByEmail(memberSignUpDto.getEmail())) {
                Director oldDirector = memberDirectorRepository.findByEmail(memberSignUpDto.getEmail()).get();
                oldDirector.update(memberSignUpDto, encodedPassword);
                memberDirectorRepository.save(oldDirector);
            } else {
                newDirector.update(memberSignUpDto, encodedPassword);
                memberDirectorRepository.save(newDirector);
            }

            redisUtil.delete(newDirector.getEmail());
        }

        return ResponseEntity.status(HttpStatus.OK).body("회원 가입에 성공하였습니다.");
    }

    // 이메일 인증
    public void requestEmailVerification(String email) {
        String code = generateVerificationCode();

        // 이메일이 공백인 경우 오류 발생
        if (email == "") {
            throw new IllegalArgumentException("400: 이메일을 입력해주세요.");
        }

        if ((memberDirectorRepository.existsByEmail(email) && !memberDirectorRepository.findByEmail(email).get().getIsDelete())
                || memberManagerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("409: 이미 존재하는 이메일입니다.");
        }

        Director director = new Director();
        director.updateVerificationCode(email, code);
        redisUtil.set(email, director, 10);

        try {
            String message = "다음 코드를 입력하여 이메일을 확인해주세요: " + code;
            sendEmail(email, "이메일 확인 코드", message);
        } catch (Exception e) {
            throw new IllegalArgumentException("400: 이메일 형식이 잘못되었습니다.");
        }
    }

    // 인증코드 발급
    private String generateVerificationCode() {
        int code = 100000 + random.nextInt(900000); // 100000에서 999999 사이의 난수 생성
        return Integer.toString(code);
    }

    // 이메일 보내기 (JavaMailSender 사용)
    public void sendEmail(String to, String subject, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        javaMailSender.send(mailMessage);
    }

    // 이메일 인증코드 확인
    public void confirmEmailVerification(String email, String code) {
        Director director = redisUtil.get(email, Director.class);

        if (director == null) {
            throw new RuntimeException("인증 시간이 초과되었습니다.");
        }

        if (!director.getVerificationCode().equals(code)) {
            throw new RuntimeException("인증 코드가 일치하지 않습니다.");
        }

        director.updateVerified(true);
        redisUtil.setInfinity(email, director);
    }

    // 감독관 정보 불러오기
    public MemberDto DirectorInfo(String id) {
        if (memberDirectorRepository.existsById(id)) {
            Director director = memberDirectorRepository.findById(id).get();

            if (director.getIsDelete()) {
                throw new IllegalArgumentException("해당 아이디는 삭제되었습니다.");
            }

            return Director.toMemberDto(director);
        } else {
            return null;
        }
    }

    // 운영자 정보 불러오기
    public MemberDto ManagerInfo(String id) {
        return Manager.toMemberDto(memberManagerRepository.findById(id).get());
    }

    // 로그인 및 토큰 발급
    public ResponseEntity<Map<String, Object>> signIn(LoginRequestDto loginRequestDto, String refreshToken, String authority) {
        Map<String, Object> response = new HashMap<>();
        Optional<Director> directorOptional;
        Optional<Manager> managerOptional;

        if (authority.equals("ROLE_DIRECTOR")) {
            directorOptional = memberDirectorRepository.findById(loginRequestDto.getId());
            directorOptional.get().updateRefreshToken(refreshToken);
            memberDirectorRepository.save(directorOptional.get());

            if (!directorOptional.get().getIsDelete()) {
                if (directorOptional.isPresent()) {
                    Director director = directorOptional.get();
                    if (passwordEncoder.matches(loginRequestDto.getPw(), director.getPw())) {
                        response.put("message", "로그인에 성공하였습니다.");
                        return ResponseEntity.status(HttpStatus.OK).body(response);
                    } else {
                        response.put("message", "비밀번호가 일치하지 않습니다.");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                    }
                } else {
                    response.put("message", "사용자를 찾을 수 없습니다.");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                }
            } else {
                response.put("message", "탈퇴한 유저입니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } else {
            managerOptional = memberManagerRepository.findById(loginRequestDto.getId());
            managerOptional.get().updateRefreshToken(refreshToken);
            memberManagerRepository.save(managerOptional.get());
            if (managerOptional.isPresent()) {
                Manager manager = managerOptional.get();
                if (passwordEncoder.matches(loginRequestDto.getPw(), manager.getPw())) {
                    response.put("message", "로그인에 성공하였습니다.");
                    return ResponseEntity.status(HttpStatus.OK).body(response);
                } else {
                    response.put("message", "비밀번호가 일치하지 않습니다.");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
            } else {
                response.put("message", "사용자를 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        }
    }

    // 운영자가 감독관 또는 운영자의 정보를 조회하는 서비스
    public MemberDto UserInfo(String memberId) {
        if (memberDirectorRepository.existsById(memberId)) {
            Director director = memberDirectorRepository.findById(memberId).get();

            if (director.getIsDelete()) {
                throw new IllegalArgumentException("해당 아이디는 삭제되었습니다.");
            }

            return Director.toMemberDto(director);
        } else if (memberManagerRepository.existsById(memberId)) {
            return Manager.toMemberDto(memberManagerRepository.findById(memberId).get());
        } else {
            throw new IllegalArgumentException("해당 아이디는 존재하지 않습니다.");
        }
    }

    // 사용자가 자신의 정보를 수정하는 서비스
    public ResponseEntity<Map<String, Object>> updateMemberByUser(String id, String authority, MemberUpdateDto memberUpdateDto) {
        Map<String, Object> response = new HashMap<>();
        Director oldDirector;
        Manager manager;

        // DB에 없는 ID를 검색하려고 하면 IllegalArgumentException
        try {
            if (authority.equals("ROLE_DIRECTOR")) {
                if (id.equals(memberUpdateDto.getId())) {
                    oldDirector = memberDirectorRepository.findById(id).get();

                    if (oldDirector.getIsDelete()) {
                        response.put("message", "해당 유저는 삭제되었습니다.");
                        response.put("code", HttpStatus.NOT_FOUND.value());
                        return ResponseEntity.ok(response);
                    }

                    Director newDirector = redisUtil.get(memberUpdateDto.getEmail(), Director.class);

                    if (!oldDirector.getEmail().equals(memberUpdateDto.getEmail()) && (memberDirectorRepository.existsByEmail(memberUpdateDto.getEmail()) || memberManagerRepository.existsByEmail(memberUpdateDto.getEmail()))) {
                        response.put("message", "이미 존재하는 이메일입니다.");
                        response.put("status", HttpStatus.BAD_REQUEST.value());

                        return ResponseEntity.ok(response);
                    }

                    if (!oldDirector.getEmail().equals(memberUpdateDto.getEmail()) && (newDirector == null || !newDirector.getIsVerified())) {
                        response.put("message", "이메일 인증을 진행해주세요.");
                        response.put("status", HttpStatus.BAD_REQUEST.value());

                        return ResponseEntity.ok(response);
                    }

                    oldDirector.update(memberUpdateDto);
                    oldDirector.updateEmail(oldDirector.getEmail());
                    memberDirectorRepository.save(oldDirector);
                    redisUtil.delete(oldDirector.getEmail());

                    response.put("message", "회원 정보가 성공적으로 수정되었습니다.");
                    response.put("code", HttpStatus.OK.value());
                    response.put("status", "success");

                    return ResponseEntity.ok(response);
                } else {
                    response.put("message", "접근 권한이 없습니다.");
                    response.put("status", HttpStatus.UNAUTHORIZED.value());

                    return ResponseEntity.ok(response);
                }
            } else {
                if (memberManagerRepository.existsById(memberUpdateDto.getId())) {
                    manager = memberManagerRepository.findById(id).get();
                    manager.update(memberUpdateDto);
                    memberManagerRepository.save(manager);
                    MemberDto updatedMemberDto = MemberDto.fromEntityByManager(manager);

                    response.put("message", "회원 정보가 성공적으로 수정되었습니다.");
                    response.put("code", HttpStatus.OK.value());
                    response.put("status", "success");

                    return ResponseEntity.ok(response);
                } else {
                    oldDirector = memberDirectorRepository.findById(memberUpdateDto.getId()).get();

                    if (oldDirector.getIsDelete()) {
                        response.put("message", "해당 계정은 삭제되었습니다.");
                        response.put("status", HttpStatus.NOT_FOUND.value());

                        return ResponseEntity.ok(response);
                    }

                    oldDirector.update(memberUpdateDto);
                    memberDirectorRepository.save(oldDirector);
                    MemberDto updatedMemberDto = MemberDto.fromEntityByDirector(oldDirector);

                    response.put("message", "회원 정보가 성공적으로 수정되었습니다.");
                    response.put("code", HttpStatus.OK.value());
                    response.put("status", "success");

                    return ResponseEntity.ok(response);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            response.put("message", memberUpdateDto.getId() + "은 존재하지 않습니다.");
            response.put("status", HttpStatus.NOT_FOUND);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    // 비밀번호 변경
    public void changePassword(MemberUpdatePasswordDto requestDto) {
        if (memberDirectorRepository.existsById(requestDto.getId())) {
            Director director = memberDirectorRepository.findById(requestDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

            if (director.getIsDelete()) {
                throw new IllegalArgumentException("해당 계정은 삭제되었습니다.");
            }

            // 기존 비밀번호 안맞으면 Exception
            if (!passwordEncoder.matches(requestDto.getOldPassword(), director.getPw())) {
                throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
            }

            director.updatePassword(passwordEncoder.encode(requestDto.getNewPassword()));
            memberDirectorRepository.save(director);
        } else {
            Manager manager = memberManagerRepository.findById(requestDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

            // 기존 비밀번호 안맞으면 Exception
            if (!passwordEncoder.matches(requestDto.getOldPassword(), manager.getPw())) {
                throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
            }

            manager.updatePassword(passwordEncoder.encode(requestDto.getNewPassword()));
            memberManagerRepository.save(manager);
        }
    }

    // 회원 탈퇴
    public void deleteMember(String id) {
        Director director = memberDirectorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(id + "인 ID는 존재하지 않습니다."));

        if (director.getIsDelete()) {
            throw new IllegalArgumentException("해당 계정은 이미 삭제되었습니다.");
        }

        director.delete();
        memberDirectorRepository.save(director);
    }

    // 아이디 찾기
    public MemberInfoDto findId(FindIdDto findIdDto) {
        MemberInfoDto responseDto = new MemberInfoDto();
        Director director;
        Manager manager;

        if (memberDirectorRepository.existsByEmail(findIdDto.getEmail())) {
            director = memberDirectorRepository.findByEmail(findIdDto.getEmail()).get();
            if (!director.getName().equals(findIdDto.getName())) {
                throw new IllegalArgumentException("이름과 이메일이 일치하지 않습니다.");
            }
            return responseDto.fromEntityByDirector(director);
        } else {
            manager = memberManagerRepository.findByEmail(findIdDto.getEmail()).get();
            if (!manager.getName().equals(findIdDto.getName())) {
                throw new IllegalArgumentException("이름과 이메일이 일치하지 않습니다.");
            }
            return responseDto.fromEntityByManager(manager);
        }
    }

    // 비밀번호 찾기
    public void findPassword(FindPasswordDto findPasswordDto) {
        String tempPassword = UUID.randomUUID().toString().split("-")[0];
        Director director;
        Manager manager;

        if (memberDirectorRepository.existsById(findPasswordDto.getId())) {
            director = memberDirectorRepository.findById(findPasswordDto.getId()).get();
            if (!director.getEmail().equals(findPasswordDto.getEmail())) {
                throw new IllegalArgumentException("아이디와 이메일이 일치하지 않습니다.");
            }

            director.updatePassword(passwordEncoder.encode(tempPassword));
            memberDirectorRepository.save(director);
        } else if (memberManagerRepository.existsById(findPasswordDto.getId())) {
            manager = memberManagerRepository.findById(findPasswordDto.getId()).get();
            if (!manager.getEmail().equals(findPasswordDto.getEmail())) {
                throw new IllegalArgumentException("아이디와 이메일이 일치하지 않습니다.");
            }

            manager.updatePassword(passwordEncoder.encode(tempPassword));
            memberManagerRepository.save(manager);
        } else {
            throw new IllegalArgumentException("아이디가 존재하지 않습니다.");
        }

        try {
            String message = "새로 등록된 임시 비밀번호로 로그인해주세요: " + tempPassword;
            sendEmail(findPasswordDto.getEmail(), "비밀번호 찾기", message);
        } catch (Exception e) {
            throw new IllegalArgumentException("이메일 형식이 잘못되었습니다.");
        }
    }

    // 운영자 회원가입을 위한 비밀번호 암호화
    public EncodedPasswordDto encodedPassword(EncodedPasswordDto encodedPasswordDto) {
        encodedPasswordDto.setPw(passwordEncoder.encode(encodedPasswordDto.getPw()));
        return encodedPasswordDto;
    }

    // 해당 아이디가 있는지 확인하는 로직
    public boolean isPresentId(String id) {
        return memberManagerRepository.existsById(id) || memberDirectorRepository.existsById(id);
    }
}
