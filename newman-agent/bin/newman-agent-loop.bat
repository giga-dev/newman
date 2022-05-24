:loop
echo Starting newman agent


call newman-agent.bat

echo Newman agent has exited, retrying in 30s
timeout /t 30 /nobreak > NUL

goto loop