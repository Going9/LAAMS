import React, { useCallback, useEffect, useState } from 'react'
import useLogin from '../../Hook/useLogin'
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';

const Home = () => {
  const [loginData,] = useState({id:localStorage.getItem("id")? localStorage.getItem("id") : "", password:""});
  const [isChecked, setChecked] = useState(localStorage.getItem("id")? true : false); 
  const [isLogin, login] = useLogin();
  const user = useSelector(state=>state.User);
  const navigate = useNavigate();
  // TODO: id, password 확인 및 로그인
  const loginButtonClick = useCallback(() => {
    if(loginData["id"] === ""){
      alert("아이디를 입력해 주세요");
      return
    }
    if(loginData["password"] === ""){
      alert("비밀번호를 입력해 주세요");
      return
    }
    login(loginData["id"], loginData["password"]);
    if(!isLogin){
      alert('로그인 정보가 잘못되었습니다.')
    }
  },[loginData,login,isLogin]);

  // TODO : 로그인 후처리 
  useEffect(()=>{
    if(isLogin){
      if(isChecked){
        localStorage.setItem("id", loginData["id"])
      }
      if(user.authority){
        switch(user.authority){
          case "ROLE_DIRECTOR":
            navigate("/director");
          break;
          case "ROLE_MANAGER":
            navigate("/manager");
          break;
        }
      }else{
        alert("권한이 설정되지 않았습니다!");
      }
    }
  },[isLogin,isChecked,loginData])


  return (
    <section className='home'>
      <div className='login-container'>
        <div className='login-logo'></div>
        <article className='login-box'>
          <div className='login-title'>Login</div>
          <form action="" className='login-form'>
          <div className='login-input-box'>
            <input 
              className='login-input'
              placeholder='ID'
              defaultValue={loginData['id']}
              onChange={e=>{
                loginData['id'] = e.target.value
              }}
            />
            <input 
              className='login-input'
              placeholder='비밀번호'
              type='password'
              onChange={e=>{
                loginData['password'] = e.target.value
              }}
            />
          </div>          
          <div className='login-checkbox'>
            <label>
              <input
                type="checkbox" 
                defaultChecked={isChecked}
                onChange={() =>{
                  setChecked(!isChecked);}}
                className='login-checkbox-input'
              />
              <span className='login-checkbox-label'>
                아이디 저장
              </span>
            </label>
          </div>
          </form>
          <button 
            className='login-btn' 
            onClick={loginButtonClick}>
            로그인
          </button>
          <div className='login-text'>
            <div>
              <div>회원가입</div>
            </div>
            <div className="login-text-right">
              <div>아이디 찾기</div>
              <div>비밀번호 찾기</div>
            </div>
          </div>
        </article>
      </div>      
    </section>
  )
}

export default Home