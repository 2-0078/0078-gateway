package com.pieceofcake.gateway.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseMessage {

    SIGN_UP_SUCCESS("회원가입에 성공하였습니다."),
    SIGN_IN_SUCCESS("로그인에 성공하였습니다."),
    SEND_VERIFICATION_MESSAGE_SUCCESS("휴대폰 인증코드 발송에 성공하였습니다."),
    VERIFY_MESSAGE_CODE_SUCCESS("휴대폰 인증코드 검증에 성공하였습니다.")
    ;

    private final String message;
}
