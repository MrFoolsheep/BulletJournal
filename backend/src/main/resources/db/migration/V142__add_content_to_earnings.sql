UPDATE template.sample_tasks
SET content = '{"delta":{"ops":[{"attributes":{"bold":true},"insert":"Date"},{"insert":": 2020-11-16\n"},{"attributes":{"bold":true},"insert":"Time"},{"insert":": 16:00:00\n"},{"attributes":{"bold":true},"insert":"Ticker"},{"insert":": BILI\n"},{"attributes":{"bold":true},"insert":"Exchange"},{"insert":": NASDAQ\n"},{"attributes":{"bold":true},"insert":"Name"},{"insert":": Bilibili\n"},{"attributes":{"bold":true},"insert":"Period"},{"insert":": Q3\n"},{"attributes":{"bold":true},"insert":"Period Year"},{"insert":": 2020\n"},{"attributes":{"bold":true},"insert":"Currency"},{"insert":": USD\n"},{"attributes":{"bold":true},"insert":"Prior EPS: "},{"insert": "-0.150\n"} ,{"attributes":{"bold":true},"insert":"Prior Revenue: "},{"insert": "260100000.000\n"} ]},"###html###":"<p><strong>Date</strong>: 2020-11-16</p><p><strong>Time</strong>: 16:00:00</p><p><strong>Ticker</strong>: BILI</p><p><strong>Exchange</strong>: NASDAQ</p><p><strong>Name</strong>: Bilibili</p><p><strong>Period</strong>: Q3</p><p><strong>Period Year</strong>: 2020</p><p><strong>Currency</strong>: USD</p><p><strong>Prior EPS</strong>: -0.150</p> <p><strong>Prior Revenue</strong>: 260100000.000</p> "}'
    WHERE id = 5005;

UPDATE template.sample_tasks
SET content = '{"delta":{"ops":[{"attributes":{"bold":true},"insert":"Date"},{"insert":": 2020-11-16\n"},{"attributes":{"bold":true},"insert":"Time"},{"insert":": 17:00:00\n"},{"attributes":{"bold":true},"insert":"Ticker"},{"insert":": GZPFY\n"},{"attributes":{"bold":true},"insert":"Exchange"},{"insert":": OTC\n"},{"attributes":{"bold":true},"insert":"Name"},{"insert":": Gazprom Neft\n"},{"attributes":{"bold":true},"insert":"Period"},{"insert":": Q3\n"},{"attributes":{"bold":true},"insert":"Period Year"},{"insert":": 2020\n"},{"attributes":{"bold":true},"insert":"Currency"},{"insert":": USD\n"},{"attributes":{"bold":true},"insert":"Prior EPS: "},{"insert":"0.360\n"},{"attributes":{"bold":true},"insert":"Prior Revenue: "},{"insert":"10508000000.000\n"}]},"mdelta":[{"attributes":{"b":true},"insert":"Date"},{"insert":": 2020-11-16\n"},{"attributes":{"b":true},"insert":"Time"},{"insert":": 17:00:00\n"},{"attributes":{"b":true},"insert":"Ticker"},{"insert":": GZPFY\n"},{"attributes":{"b":true},"insert":"Exchange"},{"insert":": OTC\n"},{"attributes":{"b":true},"insert":"Name"},{"insert":": Gazprom Neft\n"},{"attributes":{"b":true},"insert":"Period"},{"insert":": Q3\n"},{"attributes":{"b":true},"insert":"Period Year"},{"insert":": 2020\n"},{"attributes":{"b":true},"insert":"Currency"},{"insert":": USD\n"},{"attributes":{"b":true},"insert":"Prior EPS: "},{"insert":"0.360\n"},{"attributes":{"b":true},"insert":"Prior Revenue: "},{"insert":"10508000000.000\n"}],"###html###":"<p><strong>Date</strong>: 2020-11-16</p><p><strong>Time</strong>: 17:00:00</p><p><strong>Ticker</strong>: GZPFY</p><p><strong>Exchange</strong>: OTC</p><p><strong>Name</strong>: Gazprom Neft</p><p><strong>Period</strong>: Q3</p><p><strong>Period Year</strong>: 2020</p><p><strong>Currency</strong>: USD</p><p><strong>Prior EPS</strong>: 0.360</p><p><strong>Prior Revenue</strong>: 10508000000.000</p> "}'
    WHERE id = 5004;